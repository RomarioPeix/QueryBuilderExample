package br.com.vivo.aem.b2b.ecommerceequipments.servlet;

import br.com.vivo.aem.b2b.ecommerceequipments.servlet.HelpSearchModel;

import java.lang.reflect.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

/**
 * @author Jose Romario Peixoto (romarinho)
 * 
 * This servlet uses the QueryBuilder API to fetch the results from the Help (FAQ) component JCR content.
 */
@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=Ecom Equipaments - Help (FAQ) Query Builder",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/b2b-servlets/ecommerceEquipmentsHelpSearch" })
public class EcommerceEquipmentsB2BHelpSearchServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 2610051404257637265L;
	private static final Logger log = LoggerFactory.getLogger(EcommerceEquipmentsB2BHelpSearchServlet.class);
	
	@Reference
	private QueryBuilder builder;
	
	private Session session;
	
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

		try {
			Gson gson = new Gson();
			Type type = new TypeToken<List>() {
			}.getType();

			String searchText = request.getParameter("param");
			String path = request.getParameter("path");
			String matchType = request.getParameter("matchType");

			ResourceResolver resourceResolver = request.getResourceResolver();
			session = resourceResolver.adaptTo(Session.class);

			Map<String, String> predicate = new HashMap<>();

			// se o caminho nao tiver setado, colocar um por padrão
			if (path == null) {
				path = "/content/b2b/e-commerce-equipamentos/";
			}

			// checa se buscará por substring ou pela string completa
			if (matchType != null) {
				searchText = "*" + searchText + "*";

				predicate.put("group.1_fulltext", searchText);
				predicate.put("group.1_fulltext.relPath", "@text");
				predicate.put("group.2_fulltext", searchText);
				predicate.put("group.2_fulltext.relPath", "@cq:panelTitle");
			} else {
				String[] searchParts = searchText.split(" ");

				int count = 1;
				for (String part : searchParts) {
					part = "*" + part + "*";

					predicate.put("group." + (count * 2 - 1) + "_fulltext", part);
					predicate.put("group." + (count * 2 - 1) + "_fulltext.relPath", "@text");
					predicate.put("group." + (count * 2) + "_fulltext", part);
					predicate.put("group." + (count * 2) + "_fulltext.relPath", "@cq:panelTitle");
					count++;
				}
			}

			log.info(searchText);

			// valores utilizados para a busca			
            predicate.put("path", path);
			predicate.put("group.p.or", "true");
			predicate.put("orderby", "path");
			predicate.put("p.limit", "-1");

			Query query = builder.createQuery(PredicateGroup.create(predicate), session);
			
			query.setStart(0);
			query.setHitsPerPage(99);
			
			SearchResult searchResult = query.getResult();

			List<HelpSearchModel> answers = new ArrayList<HelpSearchModel>();

			int cur = 0;
			for(Hit hit : searchResult.getHits()) {
				String topic;
				String faq = hit.getNode().getProperty("cq:panelTitle").getString();

				if (hit.getNode().getProperty("sling:resourceType").getString().contains("accordion")) {
					topic = faq; 	
				} else {
					topic = hit.getNode().getParent().getProperty("cq:panelTitle").getString();
				}

				HelpSearchModel answer = new HelpSearchModel(topic, faq);
				answers.add(answer);
				cur++;
			}

			String jsonResponse = gson.toJson(answers, type);

			response.getWriter().write(jsonResponse);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			
			if(session != null) {
				
				session.logout();
			}
		}
	}

}
