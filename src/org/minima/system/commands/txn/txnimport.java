package org.minima.system.commands.txn;

import org.minima.objects.base.MiniData;
import org.minima.system.commands.Command;
import org.minima.system.commands.txn.txndb.TxnDB;
import org.minima.system.commands.txn.txndb.TxnRow;
import org.minima.utils.json.JSONObject;

public class txnimport extends Command {

	public txnimport() {
		super("txnimport","[data:] (id:) - Import a transaction. Optionally specify the ID");
	}
	
	@Override
	public JSONObject runCommand() throws Exception {
		JSONObject ret = getJSONReply();

		TxnDB db = TxnDB.getDB();
		
		String data = getParam("data");
		
		//Convert this..
		TxnRow txnrow = TxnRow.convertMiniDataVersion(new MiniData(data));
		if(existsParam("id")) {
			txnrow.setID(getParam("id"));
		}
		
		db.addCompleteTransaction(txnrow);
		
		JSONObject resp = new JSONObject();
		ret.put("response", txnrow.toJSON());
		
		return ret;
	}

	@Override
	public Command getFunction() {
		return new txnimport();
	}

}
