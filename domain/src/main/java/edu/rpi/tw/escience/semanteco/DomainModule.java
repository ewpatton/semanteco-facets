package edu.rpi.tw.escience.semanteco;

import java.net.URI;
import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;

import edu.rpi.tw.escience.semanteco.Domain;
import edu.rpi.tw.escience.semanteco.Module;
import edu.rpi.tw.escience.semanteco.ModuleConfiguration;
import edu.rpi.tw.escience.semanteco.Request;
import edu.rpi.tw.escience.semanteco.Resource;
import edu.rpi.tw.escience.semanteco.SemantEcoUI;
import edu.rpi.tw.escience.semanteco.query.Query;

public class DomainModule implements Module {

	private ModuleConfiguration config = null;
	
	@Override
	public void visit(Model model, Request request) {
		// does nothing
	}

	@Override
	public void visit(OntModel model, Request request) {
		// does nothing
	}

	@Override
	public void visit(Query query, Request request) {
		// does nothing
	}

	@Override
	public void visit(SemantEcoUI ui, Request request) {
		String responseStr = "<div id=\"DomainFacet\" class=\"facet\">";
		@SuppressWarnings("unchecked")
		List<Domain> domains = (List<Domain>)request.getParam("available-domains");
		if(domains != null) {
			for(Domain i : domains) {
				URI uri = i.getUri();
				responseStr += "<input name=\"domain\" type=\"checkbox\" checked=\"checked\" value=\""+uri.toString()+"\" />";
				responseStr += i.getLabel();
				responseStr += "<br />";
			}
		}
		responseStr += "</div>";
		Resource res = config.generateStringResource(responseStr);
		ui.addFacet(res);
	}

	@Override
	public String getName() {
		return "Domain";
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

}
