package org.minima.miniscript.functions.sha;

import org.minima.miniscript.Contract;
import org.minima.miniscript.exceptions.ExecutionException;
import org.minima.miniscript.functions.MinimaFunction;
import org.minima.miniscript.values.HEXValue;
import org.minima.miniscript.values.NumberValue;
import org.minima.miniscript.values.ScriptValue;
import org.minima.miniscript.values.Value;
import org.minima.objects.base.MiniData;
import org.minima.objects.proofs.Proof;
import org.minima.utils.Crypto;

public class CHAINSHA extends MinimaFunction {

	public CHAINSHA() {
		super("CHAINSHA");
	}
	
	@Override
	public Value runFunction(Contract zContract) throws ExecutionException {
		//Get the Input.. Could be HEX, SCRIPT, NUMBER
		Value val     = getParameter(0).getValue(zContract);
		MiniData data = val.getMiniData();

		//Get the 32 byte hash data chain + 1 byte for left right 
		HEXValue chain = (HEXValue) getParameter(1).getValue(zContract);
		
		//Bit Strength
		int bits;
		try {
			bits = Proof.getChainSHABits(chain.toString());
		} catch (Exception e) {
			throw new ExecutionException(e.toString());
		}
		
		//Hash the data
		byte[] hash = Crypto.getInstance().hashData(data.getData(), bits);
		MiniData finalhash = new MiniData(hash);
		
		//Create a proof..
		Proof chainproof = new Proof();

		//Hash the Input..		
		chainproof.setData(finalhash);
		chainproof.setProof(chain.getMiniData());
		
		//Return..
		return new HEXValue(chainproof.getFinalHash());
	}
	
	@Override
	public MinimaFunction getNewFunction() {
		return new CHAINSHA();
	}
}
