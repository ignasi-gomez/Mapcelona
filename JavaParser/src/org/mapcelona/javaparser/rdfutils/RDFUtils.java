package org.mapcelona.javaparser.rdfutils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import virtuoso.jena.driver.VirtModel;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

public class RDFUtils
{
	private ResourceBundle			rb;
	private Model					demo, city, m;
	private Resource				bcn, owlcity, owldistrict, owlbarrio, owldataclass, pieceOfData;
	private Property				ofClass, hasName, isDistrictOf, isNeighbourhoodOf,
									hasValue, hasMapping, refersTo, isOfDataClass, hasAge;
	private Map<String,Resource>	mapBarrios, mapDistritos;
	private Map<String,String>		alternates;
	
	private static final String	cityUri = "http://www.mapcelona.org/city.owl#";
	private static final String	bcnUri = "http://www.mapcelona.org/barcelona.owl#";
	private static final String	rdfUri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String demoUri = "http://www.mapcelona.org/demo.owl#";
	
	public RDFUtils()
	{
		rb = ResourceBundle.getBundle("virtuoso");
		mapBarrios = new TreeMap<String,Resource>();
		mapDistritos = new TreeMap<String,Resource>();
		connect();
		loadCity();
		
		alternates = new TreeMap<String,String>();
		alternates.put("hostafrancs", "bordeta-hostafrancs");
		alternates.put("la font de la guatlla", "font de la guatlla");
		alternates.put("el poble-sec", "poble-sec");
		alternates.put("el raval", "raval");
		alternates.put("el gotic", "gotic");
		alternates.put("sant pere, santa caterina i la ribera", "parc");
		alternates.put("la barceloneta", "barceloneta");
		alternates.put("fort pienc", "estacio nord");
		alternates.put("dreta de l'eixample", "dreta eixample");
		alternates.put("l'antiga esquerra de l'eixample", "esquerra eixample");
		alternates.put("vallvidrera, el tibidabo i les planes", "vallvidrera-les planes");
		alternates.put("sant gervasi-la bonanova", "sant gervasi");
		alternates.put("la vall d'hebron", "vall d�hebron");
		alternates.put("el guinardo", "guinardo");
		alternates.put("ciutat meridiana", "ciutat meridiana-vallbona");
		alternates.put("roquetes", "roquetes-verdum");
		alternates.put("vilapicina i torre llobeta", "vilapicina-turo de la peira");
		alternates.put("sant andreu de palomar", "sant andreu");
		alternates.put("el congres i els indians", "congres");
		alternates.put("la sagrera", "sagrera");
		alternates.put("la verneda i la pau", "verneda");
		alternates.put("el clot", "clot");
		alternates.put("el besos i el maresme", "barris besos");
		alternates.put("vallcarca i els penitents", "vallcarca");
		alternates.put("vila de gracia", "gracia");
		alternates.put("el poblenou", "poblenou");
		alternates.put("la vila olimpica del poblenou", "fort pius");
	}
	
	private void loadCity()
	{
		InputStream	in;

		city = ModelFactory.createOntologyModel();
		in = FileManager.get().open("http://www.mapcelona.org/city.owl");
		if (in == null)
		{
			throw new IllegalArgumentException("File not found");
		}
		city.read(in, null);

		demo = ModelFactory.createOntologyModel();
		in = FileManager.get().open("http://www.mapcelona.org/demo.owl");
		if (in == null)
		{
			throw new IllegalArgumentException("File not found");
		}
		demo.read(in, null);
		
		ofClass = city.getProperty(rdfUri + "type");
		owlcity = city.getResource(cityUri + "City");
		hasName = city.getProperty(cityUri + "hasName");
		owldistrict = city.getResource(cityUri + "District");
		isDistrictOf = city.getProperty(cityUri + "isDistrictOf");
		owlbarrio = city.getResource(cityUri + "Neighbourhood");
		isNeighbourhoodOf = city.getProperty(cityUri + "isNeighbourhoodOf");
		
		owldataclass = demo.getResource(demoUri + "DataClass");
		pieceOfData = demo.getResource(demoUri + "PieceOfData");
		hasMapping = demo.getProperty(demoUri + "hasMapping");
		refersTo = demo.getProperty(demoUri + "refersTo");
		isOfDataClass = demo.getProperty(demoUri + "isOfDataClass");
		hasAge = demo.getProperty(demoUri + "hasAge");
		hasValue = demo.getProperty(demoUri + "hasValue");
	}

	private void connect()
	{
		m =	VirtModel.openDatabaseModel(
			"Barcelona", // graph name
			"jdbc:virtuoso://" + rb.getString("ip") + ":1111", //1111 is the default port
			"dba", // user
			rb.getString("password") // password
			);
	}
	
	public void clean()
	{
		m.removeAll();
		bcn = m.createResource(bcnUri + "Barcelona");
		m.add(bcn, hasName, "Barcelona");
		m.add(bcn, ofClass, owlcity);
	}

	public void addDistrict(String odistrict, List<String> barrios)
	{
		String				district, barrio, alternate;
		Iterator<String>	it;
		Resource			rdfdistrict, rdfbarrio;
		
		district = normalise(odistrict);
		rdfdistrict = m.createResource(bcnUri + normaliseSpaces(district));
		m.add(rdfdistrict, hasName, district);
		m.add(rdfdistrict, ofClass, owldistrict);
		m.add(rdfdistrict, isDistrictOf, bcn);
		
		it = barrios.iterator();
		while(it.hasNext())
		{
			barrio = it.next();
			barrio = normalise(barrio);
			rdfbarrio = m.createResource(bcnUri + normaliseSpaces(barrio) + "_n");
			m.add(rdfbarrio, hasName, barrio);
			m.add(rdfbarrio, ofClass, owlbarrio);
			m.add(rdfbarrio, isNeighbourhoodOf, rdfdistrict);
			
			alternate = alternates.get(barrio);
			if(alternate != null)
			{
				System.out.println("alternate: " + alternate);
				m.add(rdfbarrio, hasName, alternate);
			}
		}
	}

	public static String normaliseSpaces(String st)
	{
		return st.replace(' ', '_');
	}

	public static String normalise(String st)
	{
		String	s;
		
		s = new String(st);
		s = s.trim();
		s = s.replaceAll("[����]","e");
	    s = s.replaceAll("[���]","u");
	    s = s.replaceAll("[���]","i");
	    s = s.replaceAll("[���]","a");
	    s = s.replaceAll("[]","o");
	    s = s.replaceAll("[�]","c");

	    s = s.replaceAll("[���]","E");
	    s = s.replaceAll("[���]","U");
	    s = s.replaceAll("[���]","I");
	    s = s.replaceAll("[���]","A");
	    s = s.replaceAll("[���]","O");
	    s = s.replaceAll("[�]","C");
	    
	    s = s.replaceAll("- ", "-");
	    s = s.replaceAll(" -", "-");
	    s = s.replaceAll("  ", " ");
	    s = s.replaceAll("\n", "");
		s = s.toLowerCase();
	
	    return s;
	}

	public void dump()
	{
		m.write(System.out, "RDF/XML");
	}

	public void getDistrict(String district)
	{
		Resource	r;
		
		r = m.getResource(bcnUri + normalise(district));
		System.out.println(r);
	}

	public Resource checkMapping(String mapping)
	{
		QuerySolution	qs;
		Resource		r;
		
		// Prepare query string
		String queryString =
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"PREFIX : <http://www.mapcelona.org/demo.owl#>\n" +
		"SELECT ?r WHERE {" +
		"?r rdf:type :DataClass.\n" +
//		"?r :hasMapping ?m" + 
//		"?r :hasMapping \"Edat mitjana edificis\"." + 
		"?r :hasMapping \"" + mapping.trim() + "\"." + 
		"}";
		// Use the ontology model to create a Dataset object
		// Note: If no reasoner has been attached to the model, no results
		// will be returned (MarriedPerson has no asserted instances)
		Dataset dataset = DatasetFactory.create(demo);
		// Parse query string and create Query object
		Query q = QueryFactory.create(queryString);
		// Execute query and obtain result set
		QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
		ResultSet resultSet = qexec.execSelect();
		
		if(resultSet.hasNext())
		{
			qs = resultSet.next();
			r = qs.getResource("r");
		}
		else
		{
			throw new UnsupportedOperationException("Mapping falla: " + mapping);
		}
		
		return r;
	}

	public Resource addDataPiece(String name, Resource dataClass, float value)
	{
		Resource	r, barrio;
		
		r = m.createResource();
		barrio = getNeighbourhood(name);
		r.addLiteral(hasValue, value);
		m.add(r, ofClass, pieceOfData);
		m.add(r, isOfDataClass, dataClass);
		m.add(r, refersTo, barrio);
		
		return r;
	}

	private Resource getNeighbourhood(String name)
	{
		Resource	r;
		
		r = mapBarrios.get(name);
		if(r == null)
		{
			QuerySolution	qs;
			
			// Prepare query string
			String queryString =
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX city: <http://www.mapcelona.org/city.owl#>\n" +
			"PREFIX : <http://www.mapcelona.org/barcelona.owl#>\n" +
			"SELECT ?r WHERE {" +
			"?r rdf:type city:Neighbourhood	.\n" +
//			"?r :hasMapping ?m" + 
//			"?r :hasMapping \"Edat mitjana edificis\"." + 
			"?r city:hasName \"" + normalise(name).trim() + "\"." + 
			"}";
			System.out.println(queryString);
			// Use the ontology model to create a Dataset object
			// Note: If no reasoner has been attached to the model, no results
			// will be returned (MarriedPerson has no asserted instances)
			Dataset dataset = DatasetFactory.create(m);
			// Parse query string and create Query object
			Query q = QueryFactory.create(queryString);
			// Execute query and obtain result set
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			ResultSet resultSet = qexec.execSelect();
			
			if(resultSet.hasNext())
			{
				qs = resultSet.next();
				r = qs.getResource("r");
			}
			else
			{
				throw new UnsupportedOperationException("Mapping falla: " + name);
			}
			
			mapBarrios.put(name, r);
		}
		
		return r;
	}

	public void setDataPieceYear(Resource dataPiece, int year)
	{
		dataPiece.addLiteral(hasAge, year);
	}

	public Resource addDataPieceDistrict(String name, Resource dataClass,
			float value)
	{
		Resource	r, barrio;
		
		r = m.createResource();
		barrio = getDistrito(name);
		r.addLiteral(hasValue, value);
		m.add(r, ofClass, pieceOfData);
		m.add(r, isOfDataClass, dataClass);
		m.add(r, refersTo, barrio);
		
		return r;
	}

	private Resource getDistrito(String name)
	{
		Resource	r;
		
		r = mapDistritos.get(name);
		if(r == null)
		{
			QuerySolution	qs;
			
			// Prepare query string
			String queryString =
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX city: <http://www.mapcelona.org/city.owl#>\n" +
			"PREFIX : <http://www.mapcelona.org/barcelona.owl#>\n" +
			"SELECT ?r WHERE {" +
			"?r rdf:type city:District	.\n" +
//			"?r :hasMapping ?m" + 
//			"?r :hasMapping \"Edat mitjana edificis\"." + 
			"?r city:hasName \"" + normalise(name).trim() + "\"." + 
			"}";
			System.out.println(queryString);
			// Use the ontology model to create a Dataset object
			// Note: If no reasoner has been attached to the model, no results
			// will be returned (MarriedPerson has no asserted instances)
			Dataset dataset = DatasetFactory.create(m);
			// Parse query string and create Query object
			Query q = QueryFactory.create(queryString);
			// Execute query and obtain result set
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			ResultSet resultSet = qexec.execSelect();
			
			if(resultSet.hasNext())
			{
				qs = resultSet.next();
				r = qs.getResource("r");
			}
			else
			{
				throw new UnsupportedOperationException("Mapping falla: " + name);
			}
			
			mapDistritos.put(name, r);
		}
		
		return r;
	}
}
