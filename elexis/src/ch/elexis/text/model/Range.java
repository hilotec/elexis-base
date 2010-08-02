package ch.elexis.text.model;

import org.eclipse.swt.graphics.Rectangle;
import org.jdom.Element;


/**
 * A Range is some part of a document. It has a position and a length within the text
 * Optionally, it can be places outside the text flow. In that case, it must provide a
 * viewport position relative to the character indicated by position. The contents of the Range is
 * totally implementation specific. It might be some text or some graphics or both.  
 * @author gerry
 *
 */
public class Range{
	public static final String ELEM_NAME="range";
	private static final String ATTR_LOCKED="locked";
	private static final String ATTR_TYPENAME="typename";
	private static final String ATTR_ID="ID";
	private static final String ATTR_PROVIDER="provider";
	public static final String ATTR_VIEWPORT="viewport";
	private static final String ATTR_LENGTH = "length";
	private static final String ATTR_START_OFFSET = "startOffset";
	
	String id;
	String typename;
	int length;
	int position;
	Rectangle viewport;
	boolean bLocked;
	
	public Range(Element el){
		id=el.getAttributeValue(ATTR_ID);
		typename=el.getAttributeValue(ATTR_TYPENAME);
		position = Integer.parseInt(el.getAttributeValue(ATTR_START_OFFSET));
		length = Integer.parseInt(el.getAttributeValue(ATTR_LENGTH));
		length=Integer.parseInt(el.getAttributeValue(""));
	}
	public Range(final int start, final int len, String typename, String id){
		length=len;
		position=start;
		this.id=id;
	}
	
	public boolean isLocked(){
		return bLocked;
	}
	public int getLength() {
		return length;
	}

	public int getPosition() {
		return position;
	}

	public void setLength(final int pos) {
		length=pos;
	}

	public void setPosition(final int pos) {
		position=pos;
	}
	public Rectangle getViewPort() {
		return null;
	}

	public Element toElement(){
		Element el=new Element(ELEM_NAME);
		el.setAttribute(ATTR_ID,id);
		el.setAttribute(ATTR_LENGTH,Integer.toString(length));
		el.setAttribute(ATTR_START_OFFSET,Integer.toString(position));
		el.setAttribute(ATTR_TYPENAME,typename);
		return el;
	}
}
