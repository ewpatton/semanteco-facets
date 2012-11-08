package edu.rpi.tw.escience.airquality.dataprovider;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;

import edu.rpi.tw.escience.waterquality.Domain;
import edu.rpi.tw.escience.waterquality.Module;
import edu.rpi.tw.escience.waterquality.ModuleConfiguration;
import edu.rpi.tw.escience.waterquality.ProvidesDomain;
import edu.rpi.tw.escience.waterquality.Request;
import edu.rpi.tw.escience.waterquality.Resource;
import edu.rpi.tw.escience.waterquality.SemantAquaUI;
import edu.rpi.tw.escience.waterquality.query.GraphComponentCollection;
import edu.rpi.tw.escience.waterquality.query.NamedGraphComponent;
import edu.rpi.tw.escience.waterquality.query.Query;
import edu.rpi.tw.escience.waterquality.query.Query.Type;
import edu.rpi.tw.escience.waterquality.query.QueryResource;
import edu.rpi.tw.escience.waterquality.query.Variable;

import static edu.rpi.tw.escience.waterquality.query.Query.RDF_NS;
import static edu.rpi.tw.escience.waterquality.query.Query.VAR_NS;

public class AirDataProviderModule implements Module, ProvidesDomain {

	private static final String SITE_VAR = "site";
	private static final String POL_NS = "http://escience.rpi.edu/ontology/semanteco/2/0/pollution.owl#";
	private static final String AIR_NS = "http://was.tw.rpi.edu/ontology/semanteco/air/air.owl#";
	public static final String QUERY_NS = "http://aquarius.tw.rpi.edu/projects/semantaqua/data-source/query-variable/";
	private static final String UNIT_NS = "http://sweet.jpl.nasa.gov/2.1/reprSciUnits.owl#";


	private static final String ISAIR_VAR = "isAir";
	private ModuleConfiguration config = null;
	private static Logger log = Logger.getLogger(AirDataProviderModule.class);
	private String countyCode = null;
	private String stateAbbr = null;
	private String stateCode = null;
	private Request request = null;

	
	@Override
	public void visit(Model model, Request request) {
		this.log = request.getLogger();
		log.debug("Visiting AirDataProviderModule building data model");
		this.config = config;
		this.request = request;
		this.stateAbbr = (String)request.getParam("state");
		if(stateAbbr == null || stateAbbr.isEmpty()) {
			throw new IllegalArgumentException("State parameter not supplied. Expected two digit state abbreviation, e.g. CA.");
		}
		try {
			this.countyCode = (String)request.getParam("county");
		}
		catch(Exception e) {
			throw new IllegalArgumentException("County parameter not supplied.", e);
		}
		
		try {
			this.stateCode = (String)request.getParam("stateCode");
		}
		catch(Exception e) {
			throw new IllegalArgumentException("County parameter not supplied.", e);
		}
		
		// TODO build air model here (cf {@link WaterDataProviderModule#visit(Model, Request)})
		final Query query = config.getQueryFactory().newQuery(Type.SELECT);
		final Variable measurement = query.getVariable(VAR_NS+"measurement");
		final QueryResource polHasCounty = query.getResource(POL_NS+"hasCounty");
		final QueryResource polHasState = query.getResource(POL_NS+"hasState");
		final QueryResource polHasCharacteristic = query.getResource(POL_NS+"hasCharacteristic");
		final QueryResource polHasValue = query.getResource(POL_NS+"hasValue");
		final Variable unit = query.getVariable(QUERY_NS+"unit");
		final Variable value = query.getVariable(QUERY_NS+"value");
		final Variable element = query.getVariable(QUERY_NS+"element");
		final QueryResource unitHasUnit = query.getResource(UNIT_NS+"hasUnit");

		String countyCode = (String) request.getParam("county");
		String stateCode = (String) request.getParam("stateCode");
		String stateAbbr = (String) request.getParam("state");
		//test values
		countyCode = "001";
		stateCode = "08";
		
		final NamedGraphComponent graph = query.getNamedGraph("http://was.tw.rpi.edu/air-measurement-data");
		graph.addPattern(measurement, polHasCounty, countyCode,null);
		graph.addPattern(measurement, polHasState, stateCode,null);
		//new patterns to test
		graph.addPattern(measurement, polHasCharacteristic, element);
		graph.addPattern(measurement, polHasValue, value);
		graph.addPattern(measurement, unitHasUnit, unit);
		
		//new construct patterns to test
		final GraphComponentCollection construct = query.getConstructComponent();
		construct.addPattern(measurement, polHasCounty, countyCode,null);
		construct.addPattern(measurement, polHasState, stateCode,null);
		construct.addPattern(measurement, polHasCharacteristic, element);
		construct.addPattern(measurement, polHasValue, value);
		construct.addPattern(measurement, unitHasUnit, unit);		
		//return true;
		//return config.getQueryExecutor(request).accept("application/json").execute(query);			
	}

	@Override
	public void visit(OntModel model, Request request) {
		request.getLogger().debug("AirDataProviderModule loading air.owl");
		model.read(AIR_NS);
	}

	@Override
	public void visit(Query query, Request request) {
		request.getLogger().debug("AirDataProviderModule updating query");
		if(query.getType() != Type.SELECT) {
			return;
		}
		final Variable site = query.getVariable(VAR_NS+SITE_VAR);
		final QueryResource rdfType = query.getResource(RDF_NS+"type");
		final QueryResource polMeasurementSite = query.getResource(POL_NS+"MeasurementSite");
		List<GraphComponentCollection> graphs = query.findGraphComponentsWithPattern(site, rdfType, polMeasurementSite);
		if(graphs != null && graphs.size() > 0) {
			query.setNamespace("air", AIR_NS);
			final Variable isAir = query.createVariableExpression("EXISTS { ?"+SITE_VAR+" a air:AirSite } as ?"+ISAIR_VAR);
			Set<Variable> vars = new LinkedHashSet<Variable>(query.getVariables());
			vars.add(isAir);
			query.setVariables(vars);
		}
	}

	@Override
	public void visit(SemantAquaUI ui, Request request) {
		Resource res = config.getResource("air-data-provider.js");
		if(res != null) {
			ui.addScript(res);
		}
	}

	@Override
	public List<Domain> getDomains(Request request) {
		log.trace("getDomains");
		List<Domain> domains = new ArrayList<Domain>();
		Domain air = config.getDomain(URI.create("http://was.tw.rpi.edu/ontology/semanteco/air/air.owl#"), true);
		air.setLabel("Air");
		addDataSources(air, request);
		addRegulations(air);
		addDataTypes(air);
		domains.add(air);
		return domains;
	}

	@Override
	public String getName() {
		return "Air Data Provider";
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public String getExtraVersion() {
		return null;
	}

	@Override
	public void setModuleConfiguration(ModuleConfiguration config) {
		this.config = config;
	}
	
	protected void addDataSources(final Domain domain, final Request request) {
		// TODO query for data sources and add them here (cf {@link WaterDataProviderModule#addDataSources(Domain, Request)})
		domain.addSource(URI.create("http://sparql.tw.rpi.edu/source/epa-gov"), "epa.gov");
	}
	
	protected void addRegulations(final Domain domain) {
		// TODO query for regulations and add them here
		domain.addRegulation(URI.create("http://was.tw.rpi.edu/ontology/semanteco/regulations/EPA-air-regulation.owl"), "EPA Regulation");
	}
	
	protected void addDataTypes(final Domain domain) {
		// change to icon names here should also occur in air-data-provider.js
		Resource res = config.getResource("clean-air.png");
		domain.addDataType("clean-air", "Clean Air", res);
		res = config.getResource("clean-air-facility.png");
		domain.addDataType("clean-air-facility", "Clean Air Facility", res);
		res = config.getResource("polluted-air");
		domain.addDataType("polluted-air", "Polluted Air", res);
		res = config.getResource("polluted-air-facility.png");
		domain.addDataType("polluted-air-facility", "Polluted Air Facility", res);
	}

}
