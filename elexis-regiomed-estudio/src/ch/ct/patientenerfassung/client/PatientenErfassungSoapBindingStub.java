// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PatientenErfassungSoapBindingStub.java

package ch.ct.patientenerfassung.client;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.axis.AxisFault;
import org.apache.axis.NoEndPointException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.client.Stub;
import org.apache.axis.utils.JavaUtils;

// Referenced classes of package ch.ct.patientenerfassung.client:
//            PatientenErfServer, ChCtPatientenerfassungPatient

public class PatientenErfassungSoapBindingStub extends Stub implements
		PatientenErfServer {

	public PatientenErfassungSoapBindingStub() throws AxisFault {
		this(null);
	}

	public PatientenErfassungSoapBindingStub(URL endpointURL,
			javax.xml.rpc.Service service) throws AxisFault {
		this(service);
		super.cachedEndpoint = endpointURL;
	}

	public PatientenErfassungSoapBindingStub(javax.xml.rpc.Service service)
			throws AxisFault {
		cachedSerClasses = new Vector();
		cachedSerQNames = new Vector();
		cachedSerFactories = new Vector();
		cachedDeserFactories = new Vector();
		if (service == null)
			super.service = new Service();
		else
			super.service = service;
		Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
		Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
		Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
		Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
		Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
		Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
		Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
		Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
		QName qName = new QName(
				"http://www.soapinterop.org/PatientenErfassung",
				"ch.ct.patientenerfassung.Patient");
		cachedSerQNames.add(qName);
		Class cls = ch.ct.patientenerfassung.client.ChCtPatientenerfassungPatient.class;
		cachedSerClasses.add(cls);
		cachedSerFactories.add(beansf);
		cachedDeserFactories.add(beandf);
	}

	private Call createCall() throws RemoteException {
		try {
			Call _call = (Call) super.service.createCall();
			if (super.maintainSessionSet)
				_call.setMaintainSession(super.maintainSession);
			if (super.cachedUsername != null)
				_call.setUsername(super.cachedUsername);
			if (super.cachedPassword != null)
				_call.setPassword(super.cachedPassword);
			if (super.cachedEndpoint != null)
				_call.setTargetEndpointAddress(super.cachedEndpoint);
			if (super.cachedTimeout != null)
				_call.setTimeout(super.cachedTimeout);
			if (super.cachedPortName != null)
				_call.setPortName(super.cachedPortName);
			for (Enumeration keys = super.cachedProperties.keys(); keys
					.hasMoreElements();) {
				String key = (String) keys.nextElement();
				if (_call.isPropertySupported(key))
					_call.setProperty(key, super.cachedProperties.get(key));
				else
					_call.setScopedProperty(key, super.cachedProperties
							.get(key));
			}

			synchronized (this) {
				if (firstCall()) {
					_call
							.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");
					for (int i = 0; i < cachedSerFactories.size(); i++) {
						Class cls = (Class) cachedSerClasses.get(i);
						QName qName = (QName) cachedSerQNames.get(i);
						Class sf = (Class) cachedSerFactories.get(i);
						Class df = (Class) cachedDeserFactories.get(i);
						_call.registerTypeMapping(cls, qName, sf, df, false);
					}

				}
			}
			Call call = _call;
			return call;
		} catch (Throwable t) {
			throw new AxisFault("Failure trying to get the Call object", t);
		}
	}

	public int speichernPatient(ChCtPatientenerfassungPatient patient,
			String jndiName) throws RemoteException {
		if (super.cachedEndpoint == null)
			throw new NoEndPointException();
		Call _call = createCall();
		_call
				.addParameter(
						new QName("", "patient"),
						new QName(
								"http://www.soapinterop.org/PatientenErfassung",
								"ch.ct.patientenerfassung.Patient"),
						ch.ct.patientenerfassung.client.ChCtPatientenerfassungPatient.class,
						ParameterMode.IN);
		_call.addParameter(new QName("", "jndiName"), new QName(
				"http://www.w3.org/2001/XMLSchema", "string"),
				java.lang.String.class, ParameterMode.IN);
		_call.setReturnType(
				new QName("http://www.w3.org/2001/XMLSchema", "int"),
				Integer.TYPE);
		_call.setUseSOAPAction(true);
		_call.setSOAPActionURI("");
		_call.setOperationStyle("rpc");
		_call
				.setOperationName(new QName(
						"https://estudio.clustertec.ch/axis/services/PatientenErfassung",
						"speichernPatient"));
		Object _resp = _call.invoke(new Object[] { patient, jndiName });
		if (_resp instanceof RemoteException)
			throw (RemoteException) _resp;
		try {
			int i = ((Integer) _resp).intValue();
			return i;
		} catch (Exception _exception) {
			int j = ((Integer) JavaUtils.convert(_resp, Integer.TYPE))
					.intValue();
			return j;
		}
	}

	private Vector cachedSerClasses;
	private Vector cachedSerQNames;
	private Vector cachedSerFactories;
	private Vector cachedDeserFactories;
}
