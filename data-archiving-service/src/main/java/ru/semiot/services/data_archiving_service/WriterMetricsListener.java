package ru.semiot.services.data_archiving_service;

import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observer;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class WriterMetricsListener implements Observer<String> {

	private static final Logger logger = LoggerFactory
			.getLogger(WriterMetricsListener.class);
	private static final String TIMESTAMP = "timestamp";
	private static final String NAME_METRIC = "name";
	private static final String ENUM_VALUE = "enum_value";
	private static final String VALUE = "value";
	private static final String OBSERVATION = "observation";
	private static final Query METRICS_QUERY = QueryFactory
			.create(new StringBuilder()
					.append("prefix hmtr: <http://purl.org/NET/ssnext/heatmeters#> ")
					.append("prefix emtr: <http://purl.org/NET/ssnext/electricmeters#> ")
					.append("prefix mcht: <http://purl.org/NET/ssnext/machinetools#> ")
					.append("prefix meter: <http://purl.org/NET/ssnext/meters/core#> ")
					.append("prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#> ")
					.append("prefix xsd: <http://www.w3.org/2001/XMLSchema#> ")
					.append("SELECT ?").append(TIMESTAMP).append(" ?")
					.append(NAME_METRIC).append(" ?").append(ENUM_VALUE)
					.append(" ?").append(OBSERVATION).append(" ?").append(VALUE)
					.append(" WHERE { {?x a hmtr:TemperatureObservation} ")
					.append("UNION{ ?x a hmtr:HeatObservation} ")
					.append("UNION{ ?x a emtr:AmperageObservation} ")
					.append("UNION{ ?x a emtr:VoltageObservation} ")
					.append("UNION{ ?x a emtr:PowerObservation} ")
					.append("UNION{ ?x a mcht:ButtonsObservation} ")
					.append("UNION{ ?x a mcht:WorkingStateObservation} ")
					.append("?x ssn:observationResultTime ?").append(TIMESTAMP)
					.append("; ").append("ssn:observedBy ?")
					.append(NAME_METRIC).append("; ")
					.append("ssn:observationResult ?result. ")
					.append("?result ssn:hasValue ?").append(ENUM_VALUE)
					.append(". ?x a ?").append(OBSERVATION)
					.append(". OPTIONAL { ?").append(ENUM_VALUE)
					.append(" meter:hasQuantityValue  ?")
					.append(VALUE).append("}}").toString());
					//TODO убрано meter:hasQuantityValue, сделать работу с ризонингом
	
	private final String nameMetric; // временное решение

	public WriterMetricsListener(String nameMetric) {
		this.nameMetric = nameMetric;
	}

	@Override
	public void onCompleted() {

	}

	@Override
	public void onError(Throwable e) {
		logger.warn(e.getMessage(), e);
	}

	@Override
	public void onNext(String message) {
		try {
			Model description = ModelFactory.createDefaultModel().read(
					new StringReader(message), null, SubscribeListener.LANG);
			if (!description.isEmpty()) {
				QueryExecution qe = QueryExecutionFactory.create(METRICS_QUERY,
						description);
				ResultSet metrics = qe.execSelect();

				while (metrics.hasNext()) {
					QuerySolution qs = metrics.next();
					// String nameMetric = qs.getResource(NAME_METRIC).getURI();
					String timestamp = qs.getLiteral(TIMESTAMP).getString();
					Literal valueLiteral = qs.getLiteral(VALUE);
					String value;
					if (valueLiteral != null) {
						value = valueLiteral.getString(); // value simulator
					} else {
						value = qs.getResource(ENUM_VALUE).getURI(); // instance enum uri 
					}
					String observation = qs.getResource(OBSERVATION).getURI();
					if (StringUtils.isNotBlank(nameMetric)
							&& StringUtils.isNotBlank(value)
							&& StringUtils.isNotBlank(observation)
							&& StringUtils.isNotBlank(timestamp)) {
						HashMap<String, String> tags = new HashMap<String, String>();
						try {
							Calendar calendar = DatatypeConverter
									.parseDateTime(timestamp);
							tags.put(OBSERVATION, observation.replaceAll(":", "_").replace("#", "-"));// _ - 
							if(valueLiteral != null) { // для симуляторов
								WriterOpenTsdb.getInstance().send(
										nameMetric, value,
										calendar.getTimeInMillis(), tags);
							}
							else {
								tags.put(VALUE, value.replaceAll(":", "_").replace("#", "-"));
								WriterOpenTsdb.getInstance().send(
										nameMetric, 0,
										calendar.getTimeInMillis(), tags);
							}
						} catch (IllegalArgumentException e) {
							logger.warn("Can't convert " + timestamp
									+ " to calendar (xsd:dateTime)");
						}
					} else {
						logger.warn("In the message not found required fields for the metric");
					}
				}

			} else {
				logger.warn("Received an empty message or in a wrong format!");
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
}