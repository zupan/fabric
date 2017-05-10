/*
Copyright DTCC, IBM 2016, 2017 All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package example;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.shim.ChaincodeHelper.newBadRequestResponse;
import static org.hyperledger.fabric.shim.ChaincodeHelper.newInternalServerErrorResponse;
import static org.hyperledger.fabric.shim.ChaincodeHelper.newSuccessResponse;

import java.io.StringReader;
import java.time.format.DateTimeFormatter;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.Response;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;


public class Example01 extends ChaincodeBase {
	
	private static Log log = LogFactory.getLog(Example01.class);

	@Override
	public Response init(ChaincodeStub stub) {
		try {
			final String function = stub.getFunction();
			switch (function) {
			case "init":
				return init(stub, stub.getParameters().stream().toArray(String[]::new));
			default:
				return newBadRequestResponse(format("Unknown function: %s", function));
			}
		} catch (NumberFormatException e) {
			return newBadRequestResponse(e.toString());
		} catch (IllegalArgumentException e) {
			return newBadRequestResponse(e.getMessage());
		} catch (Throwable e) {
			return newInternalServerErrorResponse(e);
		}
	}
	
	@Override
	public Response invoke(ChaincodeStub stub) {

		try {
			final String function = stub.getFunction();
			final String[] args = stub.getParameters().stream().toArray(String[]::new);
			
			switch (function) {
			case "invoke":
				return invoke(stub, args);
			case "delete":
				return delete(stub, args);
			case "query":
				return query(stub, args);
			case "history":
				return history(stub, args);
			default:
				return newBadRequestResponse(format("Unknown function: %s", function));
			}
		} catch (NumberFormatException e) {
			return newBadRequestResponse(e.toString());
		} catch (IllegalArgumentException e) {
			return newBadRequestResponse(e.getMessage());
		} catch (Throwable e) {
			return newInternalServerErrorResponse(e);
		}

	}

	private Response init(ChaincodeStub stub, String[] args) {
		if (args.length != 3)
		    throw new IllegalArgumentException("Incorrect number of arguments. Expecting: init(key, type, topic)");

		final String key = args[0];
		final String type = args[1];
		final String topic = args[2];

		stub.putStringState(key, Json.createObjectBuilder()
                .add("type", type)
    			.add("topic", topic)
	    		.build().toString());

		return newSuccessResponse();
	}
	
	private Response invoke(ChaincodeStub stub, String[] args) {
		if (args.length != 3)
		    throw new IllegalArgumentException("Incorrect number of arguments. Expecting: invoke(key, type, topic)");
		
		final String key = args[0];
		final String type = args[1];
		final String topic = args[2];

        if (!type.equals("sub") && !type.equals("pub"))
            throw new IllegalArgumentException("Provided type is not allowed");

        // perform the insert
        JsonObject insert = Json.createObjectBuilder()
            .add("type", type)
            .add("topic", topic)
            .build();

        stub.putStringState(key, insert.toString());

		return newSuccessResponse(insert.toString().getBytes(UTF_8));
	}

	private Response delete(ChaincodeStub stub, String args[]) {
		if (args.length != 1)
		    throw new IllegalArgumentException("Incorrect number of arguments. Expecting: delete(key)");
		
		final String key = args[0];
		
		stub.delState(key);
		
		return newSuccessResponse();
	}
	
	private Response query(ChaincodeStub stub, String[] args) {
		if (args.length != 1)
		    throw new IllegalArgumentException("Incorrect number of arguments. Expecting: query(key)");

		final String accountKey = args[0];
        JsonReader jsonReader = Json.createReader(new StringReader(stub.getStringState(accountKey)));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
		return newSuccessResponse(Json.createObjectBuilder()
				.add("Key", accountKey)
				.add("type", object.getString("type"))
				.add("topic", object.getString("topic"))
				.build().toString().getBytes(UTF_8));
    }

	private Response history(ChaincodeStub stub, String[] args) {
		if (args.length != 1)
		    throw new IllegalArgumentException("Incorrect number of arguments. Expecting: history(key)");

		final String accountKey = args[0];
        QueryResultsIterator<KeyModification> resultQuery = stub.getHistoryForKey(accountKey);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        StringBuilder result = new StringBuilder().append("[");
        while(resultQuery.iterator().hasNext()) {
            KeyModification obj = resultQuery.iterator().next();

            result.append(Json.createObjectBuilder()
                    .add("value", obj.getStringValue())
                    .add("time", formatter.format(obj.getTimestamp()))
					.build()
					.toString());

            if (resultQuery.iterator().hasNext()) result.append(",");
        }
        result.append("]");

        return newSuccessResponse(result.toString());
	}

	public static void main(String[] args) throws Exception {
		new Example01().start(args);
	}

}
