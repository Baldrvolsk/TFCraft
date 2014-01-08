package TFC.Core.Player;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import TFC.API.IClothing;
import TFC.Core.TFC_Climate;
import TFC.Core.TFC_Core;
import TFC.Core.TFC_Time;
import TFC.TileEntities.TileEntityFireEntity;

public class BodyTempStats 
{
	private int temperatureLevel = 0;
	private int prevTemperatureLevel = 0;
	private int extraFoodConsumed = 0;
	private int extraWaterConsumed = 0;
	private int heatStorage = 0;

	private long tempTimer = 0;

	Random rand = new Random();
	ItemStack itemHead,itemChest,itemLegs,itemFeet;


	public void onUpdate(EntityPlayer player)
	{
		if (TFC_Time.getTotalTicks() - this.tempTimer >= 20)
		{
			this.tempTimer += 20;

			if(heatStorage > temperatureLevel)
			{
				heatStorage--;
				//player.addChatMessage("HS: "+heatStorage);
			}



			if(!player.capabilities.isCreativeMode)
				applyTemperature(player);
		}
	}

	private void applyTemperature(EntityPlayer player)
	{
		FoodStatsTFC food = TFC_Core.getPlayerFoodStats(player);

		//Player's basic body functions.
		prevTemperatureLevel = temperatureLevel;
		if(temperatureLevel <-1){
			if(rand.nextInt(2000-getBaseBodyTempMod(player))<20 && food.foodLevel >= 500)
				temperatureLevel++;
			temperatureLevel+=applyTemperatureFromHeatSources(player);
		}
		if((player.isSprinting() || player.swingProgress != 0)&& rand.nextInt(1000- (getBaseBodyTempMod(player) )/2 ) < 20 )
			temperatureLevel++;
		if(temperatureLevel > 1 && rand.nextInt(1500 - (player.isInWater()?1000:0))<20 && food.waterLevel >= 500)
			temperatureLevel--;

		temperatureLevel += applyTemperatureFromEnvironment(player);

		//If we are warm then add it to heat reserves
		heatStorage = Math.max(Math.min(temperatureLevel+heatStorage, 14), 0);

		extraFoodConsumed = (temperatureLevel <0 && heatStorage <= 0 && rand.nextInt(350)<20)?temperatureLevel*-1:0;
		extraWaterConsumed = (temperatureLevel >0 && heatStorage <= 0 && rand.nextInt(350)<20)?temperatureLevel:0;

		if(temperatureLevel != prevTemperatureLevel && !((prevTemperatureLevel >=-1 && prevTemperatureLevel <=1)&&
				(temperatureLevel >=-1 && temperatureLevel <=1)))
			tellPlayerMessage(player);
		prevTemperatureLevel = temperatureLevel;
		if(temperatureLevel >= -1 && temperatureLevel <= 1){
			extraFoodConsumed = 0;
			extraWaterConsumed = 0;
		}
	}

	public int applyTemperatureFromEnvironment(EntityPlayer player){
		int x = (int)(player.posX);
		int y = (int)(player.posY);
		int z = (int)(player.posZ);
		float temperature = TFC_Climate.getHeightAdjustedTemp(x, y, z);
		//if it's cold
		if(temperature <=10){
			int modifier = (int)((temperature - 10)*30);
			if(rand.nextInt(1200 + modifier)<20)
				return -1;
		}
		//if it's warm
		else if(temperature >=30){
			int modifier = (int)((temperature - 30)*30);
			if(rand.nextInt(1200-modifier)<20)
				return 1;
		}
		return 0;
	}

	public int applyTemperatureFromHeatSources(EntityPlayer player){
		int x = (int)(player.posX);
		int y = (int)(player.posY);
		int z = (int)(player.posZ);
		int returnAmount = 0;
		for(int i = x - 10; i<x +10;i++)
			for(int j = y-3;j<y+3;j++)
				for(int k = z-10;k<z+10;k++){
					TileEntity te = player.worldObj.getBlockTileEntity(i, j, k);
					if((player.worldObj.getBlockId(x, y, z) == Block.lavaStill.blockID || player.worldObj.getBlockId(x, y, z) == Block.lavaMoving.blockID)
							||(te != null && te instanceof TileEntityFireEntity && ((TileEntityFireEntity)te).fireTemperature > 100))
						returnAmount += (rand.nextInt(2000 - 198*(10-( (int)player.getDistance(i, j, k) )) )<20?1:0);
				}
		return returnAmount;
	}

	public int getBaseBodyTempMod(EntityPlayer player){
		itemHead = player.inventory.armorItemInSlot(3);
		itemChest = player.inventory.armorItemInSlot(2);
		itemLegs = player.inventory.armorItemInSlot(1);
		itemFeet = player.inventory.armorItemInSlot(0);
		int returnAmount = 0;
		if(itemHead !=null)
			returnAmount += ((IClothing)itemHead.getItem()).getThermal();
		if(itemChest !=null)
			returnAmount += ((IClothing)itemChest.getItem()).getThermal();
		if(itemLegs !=null)
			returnAmount += ((IClothing)itemLegs.getItem()).getThermal();
		if(itemFeet !=null)
			returnAmount += ((IClothing)itemFeet.getItem()).getThermal();
		return returnAmount;
	}

	private void tellPlayerMessage(EntityPlayer player){
		switch(temperatureLevel){
		case -1:
		case 0:
		case 1: player.addChatMessage("You feel comfortable");break;
		case -2: player.addChatMessage("You feel cool.");break;
		case -3: player.addChatMessage("You feel chilled.");break;
		case -4: player.addChatMessage("You feel cold.");break;
		case -5: player.addChatMessage("You feel numb.");break;
		case -6: player.addChatMessage("You feel freezing!");break;
		case -7: killPlayer(player);break;
		case 2: player.addChatMessage("You feel warm.");break;
		case 3: player.addChatMessage("You feel very warm.");break;
		case 4: player.addChatMessage("You feel hot.");break;
		case 5: player.addChatMessage("You feel very hot.");break;
		case 6: player.addChatMessage("You feel extremely hot!");break;
		case 7: killPlayer(player);break;
		}
	}

	private void killPlayer(EntityPlayer player){
		player.inventory.dropAllItems();
		player.setHealth(0);		
	}

	public void readNBT(NBTTagCompound nbt)
	{
		if (nbt.hasKey("tempCompound"))
		{
			NBTTagCompound tempCompound = nbt.getCompoundTag("tempCompound");
			this.temperatureLevel = tempCompound.getInteger("tempLev");
			this.extraWaterConsumed = tempCompound.getInteger("waterExtra");
			this.extraFoodConsumed = tempCompound.getInteger("foodExtra");
			this.heatStorage = tempCompound.getInteger("heatStorage");
			this.tempTimer = tempCompound.getLong("tempTimer");
		}
	}

	/**
	 * Writes food stats to an NBT object.
	 */
	public void writeNBT(NBTTagCompound nbt)
	{
		NBTTagCompound tempCompound = new NBTTagCompound();
		tempCompound.setInteger("tempLev",this.temperatureLevel);
		tempCompound.setInteger("waterExtra",this.extraWaterConsumed);
		tempCompound.setInteger("foodExtra",this.extraFoodConsumed);
		tempCompound.setInteger("heatStorage",this.heatStorage);
		tempCompound.setLong("tempTimer",this.tempTimer);
		nbt.setCompoundTag("tempCompound", tempCompound);
	}

	public int getExtraFood()
	{
		return extraFoodConsumed;
	}

	public int getExtraWater()
	{
		return extraWaterConsumed;
	}
}