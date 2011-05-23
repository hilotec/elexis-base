package ch.elexis.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.ui.statushandlers.StatusManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;

import ch.elexis.Hub;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLinkException;

public class Test_PersistentObject extends AbstractPersistentObjectTest {
	
	private JdbcLink link;

	@Before
	public void setUp(){
		link = initDB();
	}
	
	@After
	public void tearDown(){
		try {
			link.exec("DROP ALL OBJECTS");		
			link.disconnect();
		} catch (JdbcLinkException je) {
			// just tell what happend and resume
			// excpetion is allowed for tests which get rid of the connection on their own
			// for example testConnect(), ...
			je.printStackTrace();
		}
	}
	
	@Test
	public void testConnect(){
		boolean ret = PersistentObject.connect(link);
		assertTrue(ret);
		PersistentObject.disconnect();
	}
	
	@Test
	public void testConnectFail(){
		// this connect methods opens its own JdbcLink by all means
		// it is looking for a demo db:
		// File demo = new File(base.getParentFile().getParent() + "/demoDB");
		
		// then for dom SWTBot related db:
		// String template = System.getProperty("SWTBot-DBTemplate");
		// File dbDir = new File(Hub.getTempDir(), "Elexis-SWTBot");
		
		// then from some user provided config
		// String connection = Hub.getCfgVariant();
		
		// then if provider is Medelexis the db wizard is opened else
		// look for db at default location
		// String d = PreferenceInitializer.getDefaultDBPath();
		
		// this is nice for runtime but makes testing really hard :)
		// we need to mock JdbcLink.createH2Link to stop creation of database
		PowerMockito.mockStatic(JdbcLink.class);
		PowerMockito.when(JdbcLink.createH2Link(Matchers.anyString())).thenReturn(
			new JdbcLink("", "", ""));
		// connect and simulate db creation failure with JdbcLink mock
		try {
			PersistentObject.connect(Hub.localCfg, null);
			fail("Expected Exception not thrown!");
		} catch (PersistenceException pe) {

		}
	}
	
	@Test
	public void testGet(){
		PersistentObjectImpl impl = new PersistentObjectImpl();
		String ret = impl.get("TestGet");
		assertNotNull(ret);
		assertEquals("test", ret);
	}
	
	@Test
	public void testState(){
		PersistentObjectImpl impl = new PersistentObjectImpl();
		impl.tablename = "abc";
		int ret = impl.state();
		assertEquals(PersistentObject.INEXISTENT, ret);
	}
	
	@Test
	public void testStoreToString(){
		PersistentObjectImpl impl = new PersistentObjectImpl();
		String ret = impl.storeToString();
		assertNotNull(ret);
		assertTrue(ret.startsWith("ch.elexis.data.Test_PersistentObject"));
	}
	
	@Test
	public void testGetXid(){
		PersistentObjectImpl impl = new PersistentObjectImpl();
		Xid ret = impl.getXid();
		assertNotNull(ret);
	}
	
	@Test
	public void testAddXid(){
		PersistentObjectImpl impl = new PersistentObjectImpl();
		Xid.localRegisterXIDDomain("test", "test", 1);
		boolean ret = impl.addXid("test", "addXid", false);
		assertTrue(ret);
		Xid id = impl.getXid();
		assertNotNull(id);
	}
	
	@Test
	public void testGetFail(){
		// mock a status manager for ignoring the error status
		StatusManager statusMock = PowerMockito.mock(StatusManager.class);
		PowerMockito.mockStatic(StatusManager.class);
		PowerMockito.when(StatusManager.getManager()).thenReturn(
				statusMock);
		
		PersistentObjectImpl impl = new PersistentObjectImpl();
		try {
			String ret = impl.get("");
			assertNotNull(ret);
			assertEquals(PersistentObject.MAPPING_ERROR_MARKER + "**", ret);
		} catch (PersistenceException pe) {

		}
		
		// if we pass ID we should get to code that reaches into the db
		// we have no table specified so a JdbcLinkException is expected
		try {
			impl.get("ID");
			fail("Expected Exception not thrown!");
		} catch (PersistenceException pe) {

		}
	}
	
	private class PersistentObjectImpl extends PersistentObject {
		
		String tablename;
		
		@SuppressWarnings("unused")
		public String getTestGet(){
			return "test";
		}
		
		@Override
		public String getLabel(){
			return null;
		}
		
		@Override
		protected String getTableName(){
			return tablename;
		}
		
	}
}
