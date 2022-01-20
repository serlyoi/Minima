package org.minima.system.commands.search;

import java.util.ArrayList;

import org.minima.database.MinimaDB;
import org.minima.database.txpowtree.TxPoWTreeNode;
import org.minima.database.wallet.KeyRow;
import org.minima.database.wallet.Wallet;
import org.minima.objects.Coin;
import org.minima.objects.base.MiniData;
import org.minima.system.brains.TxPoWSearcher;
import org.minima.system.commands.Command;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

public class coins extends Command {

	public coins() {
		super("coins","(relevant:true) (simple:true) (coinid:) (address:) (tokenid:) - Search for specific coins");
	}
	
	@Override
	public JSONObject runCommand() throws Exception{
		JSONObject ret = getJSONReply();
		
		//Check a parameter specified
		if(!existsParam("relevant") && !existsParam("coinid") && !existsParam("address") && !existsParam("tokenid")) {
			throw new Exception("No parameters specified");
		}
		
		//Get the txpowid
		boolean relevant	= existsParam("relevant");
		boolean simple		= getBooleanParam("simple",false);
		
		boolean scoinid		= existsParam("coinid");
		MiniData coinid		= MiniData.ZERO_TXPOWID;
		if(scoinid) {
			coinid = new MiniData(getParam("coinid", "0x01"));
		}
		
		boolean saddress	= existsParam("address");
		MiniData address	= MiniData.ZERO_TXPOWID;
		if(saddress) {
			address = new MiniData(getParam("address", "0x01"));
		}
		
		boolean stokenid	= existsParam("tokenid");
		MiniData tokenid	= MiniData.ZERO_TXPOWID;
		if(stokenid) {
			tokenid = new MiniData(getParam("tokenid", "0x01"));
		}
		
		//Get the tree tip..
		TxPoWTreeNode tip = MinimaDB.getDB().getTxPoWTree().getTip();
		
		//Run the query
		ArrayList<Coin> coins = TxPoWSearcher.searchCoins(	tip, relevant, 
															scoinid, coinid, 
															saddress, address, 
															stokenid, tokenid);
		
		
		//Are we only showing simple Coins..
		ArrayList<Coin> finalcoins = coins;
		if(simple) {
			finalcoins = new ArrayList<>();
			
			//Get the wallet..
			Wallet wallet = MinimaDB.getDB().getWallet();
			
			//Get all the keys
			ArrayList<KeyRow> keys = wallet.getAllRelevant();
			
			//Now cycle through the coins
			for(Coin cc : coins) {
				for(KeyRow kr : keys) {
					//Is it a simple key
					if(!kr.getPublicKey().equals("")) {
						if(cc.getAddress().isEqual(new MiniData(kr.getAddress()))) {
							finalcoins.add(cc);
						}
					}
				}
			}
		}
		
		//Put it all in an array
		JSONArray coinarr = new JSONArray();
		for(Coin cc : finalcoins) {
			coinarr.add(cc.toJSON());
		}
		
		ret.put("status", true);
		ret.put("response", coinarr);
	
		return ret;
	}

	@Override
	public Command getFunction() {
		return new coins();
	}

}
