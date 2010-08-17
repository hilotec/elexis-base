package ch.elexis.text;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import ch.elexis.Desk;
import ch.elexis.ElexisException;
import ch.elexis.text.model.Range;

/**
 * An IRangeRenderer that handles only some markups 
 * @author Gerry Weirich
 *
 */
public class DefaultRenderer implements IRangeRenderer {

	@Override
	public boolean canRender(String rangeType, OUTPUT outputType) {
		if (rangeType.equals(Range.TYPE_MARKUP)) {
			if (outputType.equals(OUTPUT.STYLED_TEXT)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object doRender(Range range, OUTPUT outputType, IRichTextDisplay rt)
			throws ElexisException {
		if (range.getType().equals(Range.TYPE_MARKUP)) {
			StyleRange sr = new StyleRange();
			sr.start = range.getPosition();
			sr.length = range.getLength();
			String idset = range.getID();
			int style = SWT.NORMAL;
			for (String id : idset.split(",")) {
				if (id.equals(Range.STYLE_BOLD)) {
					style |= SWT.BOLD;
				} else if (id.equals(Range.STYLE_ITALIC)) {
					style |= SWT.ITALIC;
				} else if (id.startsWith(Range.STYLE_FOREGROUND)) {
					sr.foreground = Desk.getColorFromRGB(id
							.substring(Range.STYLE_FOREGROUND.length() + 1));
				} else if (id.equals("underline")) {
					sr.underline = true;
				}
				
			}
			sr.fontStyle=style;
			return sr;
		} else {
			throw new ElexisException(getClass(), range.getType()
					+ " not supported", ElexisException.EE_NOT_SUPPORTED);
		}
	}

	@Override
	public IAction[] getActions(String rangeType) {
		return null;
	}

}
