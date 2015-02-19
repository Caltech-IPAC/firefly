/*
 * Copyright 2009 Richard Zschech.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.zschech.gwt.comet.client;

import java.io.Serializable;

import com.google.gwt.rpc.client.impl.ClientWriterFactory;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader;
import com.google.gwt.user.client.rpc.impl.Serializer;

/**
 * The base class for comet serializers. To instantiate this class follow this example:
 * 
 * <code>
 * @SerialTypes({ MyType1.class, MyType2.class })
 * public static abstract class MyCometSerializer extends CometSerializer {}
 * 
 * CometSerializer serializer = GWT.create(MyCometSerializer.class);
 * serializer.parse(...);
 * </code>
 * 
 * Where MyType1 and MyType2 are the types that your expecting to receive from the server.
 * 
 * @author Richard Zschech
 */
public abstract class CometSerializer {
	
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T parse(String message) throws SerializationException {
		if (getMode() == SerialMode.RPC) {
			try {
				Serializer serializer = getSerializer();
				ClientSerializationStreamReader reader = new ClientSerializationStreamReader(serializer);
				reader.prepareToRead(message);
				return (T) reader.readObject();
			}
			catch (RuntimeException e) {
				throw new SerializationException(e);
			}
		}
		else {
			try {
				SerializationStreamReader reader = ClientWriterFactory.createReader(message);
				return (T) reader.readObject();
			}
			catch (RuntimeException e) {
				throw new SerializationException(e);
			}
		}
	}
	
	protected abstract Serializer getSerializer();
	
	public abstract SerialMode getMode();
}
