package ch.elexis.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import ch.elexis.data.TarmedLeistung;
import ch.rgw.tools.TimeTool;

public class TarmedValidDateFilter extends ViewerFilter {
	
	private TimeTool validDate;
	private boolean doFilter = false;
	
	/**
	 * Show only positions valid at date. Set date to null to show all.
	 * 
	 * @param date
	 */
	public void setValidDate(TimeTool date){
		validDate = date;
	}
	
	public boolean getDoFilter(){
		return doFilter;
	}
	
	public void setDoFilter(boolean value){
		doFilter = value;
	}


	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element){
		TarmedLeistung leistung = (TarmedLeistung) element;
		
		if (doFilter && validDate != null) {
			TimeTool validFrom = leistung.getGueltigVon();
			TimeTool validTo = leistung.getGueltigBis();
			// Kapitel do not have valid dates
			if (validFrom != null && validTo != null) {
				if (!(validDate.isAfterOrEqual(validFrom) && validDate.isBeforeOrEqual(validTo)))
					return false;
			}
		}

		return true;
	}
}
