package manager;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import model.Bodega;
import model.Campo;
import model.Entrada;
import model.Vid;
import utils.TipoVid;

public class Manager {
	private static Manager manager;
	private ArrayList<Entrada> entradas;
	private Session session;
	private Transaction tx;
	private Bodega b;
	private Campo c;
	MongoCollection<Document> collection;
	MongoDatabase database;

	private Manager () {
		this.entradas = new ArrayList<>();
	}

	public static Manager getInstance() {
		if (manager == null) {
			manager = new Manager();
		}
		return manager;
	}

	private void connectSession() {
		org.hibernate.SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
    	session = sessionFactory.openSession();
		
    	// Conexión con el servidor de MongoDB
        String uri = "mongodb://localhost:27017";
        MongoClientURI mongoClientURI = new MongoClientURI(uri);        
        MongoClient mongoClient = new MongoClient (mongoClientURI);

        // Selecciona la base de datos
        database = mongoClient.getDatabase("dam2tm06uf2p2");
	}

	public void init() {
		connectSession();
		getEntrada();
		manageActions();
		showAllCampos();
		session.close();
	}

	private void manageActions() {
		for (Entrada entrada : this.entradas) {
			try {
				System.out.println(entrada.getInstruccion());
				String instruccion = entrada.getInstruccion();
				if (instruccion != null) {				
				switch (entrada.getInstruccion().toUpperCase().split(" ")[0]) {
					case "B":
						addBodega(entrada.getInstruccion().split(" "));
						break;
					case "C":
						addCampo(entrada.getInstruccion().split(" "));
						break;
					case "V":
						addVid(entrada.getInstruccion().split(" "));
						break;
					case "#":
						vendimia();
						break;
					default:
						System.out.println("Instruccion incorrecta");
				}
				}
			} catch (HibernateException e) {
				e.printStackTrace();
				if (tx != null) {
					tx.rollback();
				}
			}
		}
	}

	private void vendimia() {
		this.b.getVids().addAll(this.c.getVids());

		tx = session.beginTransaction();
		session.save(b);

		tx.commit();
	}
	
	public void addVid(String[] split) {
		Vid v = new Vid(TipoVid.valueOf(split[1].toUpperCase()), Integer.parseInt(split[2]));
		collection = database.getCollection("campo");
		Document lastVineyard = collection.find().sort(new Document("_id", -1)).first();
		collection = database.getCollection("vid");
		Document document = new Document().append("type", v.getVid().toString()).append("quantity", v.getCantidad()).append("campo", lastVineyard);
		collection.insertOne(document);

		Document document2 = new Document().append("type", v.getVid().toString()).append("quantity", v.getCantidad());
		collection = database.getCollection("campo");

		Document update = new Document("$push", new Document("vid", document2));
		collection.updateOne(lastVineyard, update);
		}

	
/*
	private void addVid(String[] split) {
		Vid v = new Vid(TipoVid.valueOf(split[1].toUpperCase()), Integer.parseInt(split[2]));
		tx = session.beginTransaction();
		session.save(v);

		c.addVid(v);
		session.save(c);

		tx.commit();

	}
*/	
	public void addCampo(String[] split) {
		collection = database.getCollection("bodega");
		Document lastBodega = collection.find().sort(new Document("_id", -1)).first();
		
		collection = database.getCollection("campo");
		Document document = new Document().append("collected", false).append("bodega", lastBodega);
		collection.insertOne(document);
	}
	
	/*
	private void addCampo(String[] split) {
		c = new Campo(b);
		tx = session.beginTransaction();

		int id = (Integer) session.save(c);
		c = session.get(Campo.class, id);

		tx.commit();
	}
	*/

	public void addBodega(String[] split) {
		b = new Bodega(split[1]);
		collection = database.getCollection("bodega");
		Document document = new Document().append("name", b.getNombre());
		//Document document = new Document().append("nombre", split[1]).append("denominacion", split[2]);
		collection.insertOne(document);

	}
/*
	private void addBodega(String[] split) {
	    if (split.length >= 3) { // Verifica si hay al menos 3 elementos en el array
	        b = new Bodega(split[1], split[2]);
	        tx = session.beginTransaction();

	        int id = (Integer) session.save(b);
	        b = session.get(Bodega.class, id);

	        tx.commit();
	    } else {
	        System.out.println("Instrucción incorrecta para agregar Bodega");
	        // Puedes manejar el error de alguna manera adecuada aquí
	    }
	}
*/

	public ArrayList<Entrada> getEntrada() {
		collection = database.getCollection("entrada");
				
		
		for (Document document : collection.find()) {
			
			Entrada entrada = new Entrada();
			entrada.setInstruccion(document.getString("action"));
			if(entrada != null)
			{
				entradas.add(entrada);
			}
		}
		return entradas;
	}
	/*
	private void getEntrada() {
		tx = session.beginTransaction();
		Query q = session.createQuery("select e from Entrada e");
		this.entradas.addAll(q.list());
		tx.commit();
	}
	*/

	private void showAllCampos() {
		tx = session.beginTransaction();
		Query q = session.createQuery("select c from Campo c");
		List<Campo> list = q.list();
		for (Campo c : list) {
			System.out.println(c);
		}
		tx.commit();
	}


}
