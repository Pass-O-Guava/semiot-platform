package ru.semiot.platform.drivers.simulator.resources;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.RiotException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.semiot.platform.deviceproxyservice.api.drivers.Device;
import ru.semiot.platform.drivers.simulator.CoAPInterface;
import ru.semiot.platform.drivers.simulator.DeviceDriverImpl;
import ru.semiot.platform.drivers.simulator.handlers.coap.DeviceHandler;
import ru.semiot.platform.drivers.simulator.handlers.coap.NewObservationHandler;
import ru.semiot.semiot.commons.namespaces.EMTR;
import ru.semiot.semiot.commons.namespaces.HMTR;
import ru.semiot.semiot.commons.namespaces.RDF;
import ru.semiot.semiot.commons.namespaces.SSN;
import ru.semiot.semiot.commons.namespaces.SSNCOM;

public class RegisterResource extends CoapResource {

	private static final String templateWampTopic = "ws://wamprouter/ws?topic=${topic}";
	private static final String queryFile = "/ru/semiot/services/deviceproxy/handlers/wamp/NewDeviceHandler/query.sparql";
	private final Model schema;
	private final Query query;
	private static final String VAR_COAP = "coap";
	private static final String VAR_WAMP = "wamp";
	private static final String VAR_SYSTEM = "system";
	private static final String WAMP = "WAMP";
	DeviceDriverImpl deviceDriverImpl;

	public RegisterResource(DeviceDriverImpl deviceDriverImpl) {
		super("register");
		System.out.println("register resoure");
		this.deviceDriverImpl = deviceDriverImpl;

		this.schema = FileManager.get().loadModel(SSN.URI);
		this.schema.add(FileManager.get().loadModel(HMTR.URI));
		this.schema.add(FileManager.get().loadModel(EMTR.URI));
		try {
			this.query = QueryFactory.create(IOUtils.toString(this.getClass()
					.getResourceAsStream(queryFile)));
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			throw new IllegalArgumentException(ex);
		}
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		System.out.println("handlePost");
		Model description = toModel(exchange.getRequestText());

		if (!description.isEmpty()) {
			mapEndpoints(description);

			addDevice(description);

			exchange.respond(CoAP.ResponseCode.CREATED);
		} else {
			System.out
					.println("Received a request without payload or in wrong format");
			exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
		}
	}

	private void addDevice(Model description) {
		try {
			if (!description.isEmpty()) {
				InfModel infModel = ModelFactory.createRDFSModel(schema,
						description);

				try (QueryExecution qexec = QueryExecutionFactory.create(query,
						infModel)) {
					final ResultSet results = qexec.execSelect();

					String systemURI = StringUtils.EMPTY; // с расчетом,
															// что в 1
															// сообщении 1
															// устройство
					while (results.hasNext()) {
						final QuerySolution soln = results.next();

						final Resource coap = soln.getResource(VAR_COAP);
						final Resource wamp = soln.getResource(VAR_WAMP);
						if (systemURI.isEmpty()) {
							final Resource system = soln
									.getResource(VAR_SYSTEM);
							systemURI = system.getURI();
						}

						System.out.println("Mapping " + coap.getURI() +" to "+ wamp.getURI());

						String wampTopic = getWampTopic(wamp.getURI());

						final NewObservationHandler handler = new NewObservationHandler(
								deviceDriverImpl, wampTopic, systemURI);

						final CoapClient coapClient = new CoapClient(
								coap.getURI());
						coapClient.setEndpoint(CoAPInterface.getEndpoint());
						final CoapObserveRelation rel = coapClient
								.observe(handler);

						// So the handler could cancel the subscription.
						handler.setRelation(rel);
						coapClient.shutdown();
						Device newDevice = new Device(systemURI, toString(description));
						if(!deviceDriverImpl.contains(newDevice)) {
							deviceDriverImpl.addDevice(newDevice);
						}
						if (!DeviceHandler.getInstance().containsHandler(
								systemURI, handler)) {
							DeviceHandler.getInstance().addHandler(systemURI,
									handler);
						}
						
					}
				}
			} else {
				System.out.println("Received an empty message or in a wrong format!");
			}
		} catch (RiotException ex) {
			System.out.println(ex.getMessage());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		super.handleDELETE(exchange); // delete from list??
	}

	private Model toModel(final String payload) {
		return ModelFactory.createDefaultModel().read(
				IOUtils.toInputStream(payload), null,
				deviceDriverImpl.getWampMessageFormat());
	}

	private void mapEndpoints(final Model source) {
		final Model tmp = ModelFactory.createDefaultModel();
		ResIterator sensors = source
				.listResourcesWithProperty(SSNCOM.hasCommunicationEndpoint);

		sensors.forEachRemaining((Resource sensor) -> {
			StmtIterator stmts = sensor
					.listProperties(SSNCOM.hasCommunicationEndpoint);

			stmts.forEachRemaining((Statement stmt) -> {
				final String uri = stmt.getObject().asResource().getURI();
				final Resource wampEndpoint = ResourceFactory
						.createResource(templateWampTopic.replace("${topic}",
								coapUriToWAMPUri(uri)));

				// Declare a new CommunicationEndpoint (WAMP)
				tmp.add(sensor, SSNCOM.hasCommunicationEndpoint, wampEndpoint)
						.add(wampEndpoint, RDF.type,
								SSNCOM.CommunicationEndpoint)
						.add(wampEndpoint, SSNCOM.protocol, WAMP);
			});
		});

		source.add(tmp);
	}

	private String coapUriToWAMPUri(final String coapUri) {
		return coapUri.replaceAll("coap://", "").replaceAll(":|/", ".");
	}

	private String toString(final Model model) {
		try (Writer writer = new StringWriter()) {
			model.write(writer, deviceDriverImpl.getWampMessageFormat());
			return writer.toString();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		return null;
	}

	private String getWampTopic(final String wampUri) {
		return wampUri.split("\\?topic=")[1];
	}
}