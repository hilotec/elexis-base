package ch.elexis.text.model;

import ch.elexis.exchange.text.IMarkup;

public class Markup extends Range implements IMarkup {
	IMarkup.TYPE type;
	
	public Markup(int pos, int length, IMarkup.TYPE type){
		super(pos,length);
		this.type=type;
	}
	@Override
	public TYPE getType() {
		return type;
	}

}
