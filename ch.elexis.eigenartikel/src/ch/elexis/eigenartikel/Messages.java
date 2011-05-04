package ch.elexis.eigenartikel;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.eigenartikel.messages"; //$NON-NLS-1$
	public static String EigenartikelDisplay_actualOnStockPacks;
	public static String EigenartikelDisplay_actualOnStockPieces;
	public static String EigenartikelDisplay_buyPrice;
	public static String EigenartikelDisplay_dealer;
	public static String EigenartikelDisplay_displayTitle;
	public static String EigenartikelDisplay_group;
	public static String EigenartikelDisplay_maxOnStock;
	public static String EigenartikelDisplay_minOnStock;
	public static String EigenartikelDisplay_PiecesPerDose;
	public static String EigenartikelDisplay_PiecesPerPack;
	public static String EigenartikelDisplay_pleaseChooseDealer;
	public static String EigenartikelDisplay_sellPrice;
	public static String EigenartikelDisplay_typ;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
