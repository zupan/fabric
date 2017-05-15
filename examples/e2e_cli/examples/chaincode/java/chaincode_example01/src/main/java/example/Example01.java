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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.Response;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;


public class Example01 extends ChaincodeBase {
	
//	private static Log log = LogFactory.getLog(Example01.class);

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
            case "queryByProperty":
				return queryByProperty(stub, args);
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

		Entry e = new Entry(args[0],
                            args[1],
                            args[2].toLowerCase().equals("publisher") ? ClientRole.Publisher : ClientRole.Subscriber,
                            new Timestamp(Calendar.getInstance().getTime().getTime()));

		stub.putStringState(args[0], e.toJSON());

		return newSuccessResponse();
	}
	
	private Response invoke(ChaincodeStub stub, String[] args) {
		return init(stub, args);
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

		return newSuccessResponse(stub.getStringState(args[0]).getBytes(UTF_8));
    }

    private Response queryByProperty(ChaincodeStub stub, String[] args) {
        String property = args[0];
        String value = args[1];

        String query = String.format("{\"selector\":{\"%s\":\"%s\"}}", property, value);

        QueryResultsIterator<KeyValue> resultQuery = stub.getQueryResult(query);

        Iterator<KeyValue> iterator = resultQuery.iterator();
        List<Entry> result = new ArrayList<>();

        // DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        while(iterator.hasNext()) {
            KeyValue obj = iterator.next();
            result.add(new Entry().fromJSON(obj.getStringValue()));
        }

        return newSuccessResponse(new Gson().toJson(result, Entry.class));
    }

	private Response history(ChaincodeStub stub, String[] args) {
		if (args.length != 1)
		    throw new IllegalArgumentException("Incorrect number of arguments. Expecting: history(key)");

		final String accountKey = args[0];
        QueryResultsIterator<KeyModification> resultQuery = stub.getHistoryForKey(accountKey);

        Iterator<KeyModification> iterator = resultQuery.iterator();
        List<Entry> result = new ArrayList<>();

        while(iterator.hasNext()) {
            KeyModification obj = iterator.next();
            result.add(new Entry().fromJSON(obj.getStringValue()));
        }

        return newSuccessResponse(new Gson().toJson(result, Entry.class));
	}

	public static void main(String[] args) throws Exception {
		new Example01().start(args);
	}

}
