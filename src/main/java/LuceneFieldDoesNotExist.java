import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.IOException;

public class LuceneFieldDoesNotExist {
    public static void main(String[] args) throws IOException, ParseException {
        fieldExistString();
    }

    public static void fieldExistString() throws IOException, ParseException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter writer = new IndexWriter(index, config);

        // Add document 1
        {
            Document doc = new Document();
            doc.add(new TextField("title", "First Title", Field.Store.YES));
            doc.add(new TextField("source", "wiki", Field.Store.YES));
            doc.add(new TextField("author", "anonymous", Field.Store.YES));

            // Add meta field that allows us to implement Does Not Exist
            addMetaFieldNames(doc);
            writer.addDocument(doc);
        }

        // Add document 2
        {
            Document doc = new Document();
            doc.add(new TextField("title", "Second Title", Field.Store.YES));
            doc.add(new TextField("origin", "paper", Field.Store.YES));
            doc.add(new TextField("author", "Spear", Field.Store.YES));

            // Add meta field that allows us to implement Does Not Exist
            addMetaFieldNames(doc);
            writer.addDocument(doc);
        }
        writer.close();

        // Field Exists using native Lucene way.
        {
            System.out.println("Query 'source' Field Exists using native Lucene way.");
            QueryParser queryParser = new QueryParser("source", analyzer);
            // Note that you must allow leading wildcard to be able to execute this query.
            queryParser.setAllowLeadingWildcard(true);
            String queryStr = "*";
            Query q = queryParser.parse(queryStr);
            queryAndDisplay(q, index);
        }


        // Field Exists using meta field.
        {
            System.out.println("Query 'author' Field Exists using meta field 'field_names'.");
            QueryParser queryParser = new QueryParser("field_names", analyzer);
            String queryStr = "+author";
            Query q = queryParser.parse(queryStr);
            queryAndDisplay(q, index);
        }

        // Field Does Not Exist using meta field.
        {
            System.out.println("Query 'origin' Field Does Not Exist using meta field 'field_names'.");
            QueryParser queryParser = new QueryParser("field_names", analyzer);
            // Note that you must allow leading wildcard to be able to execute this query.
            queryParser.setAllowLeadingWildcard(true);
            String queryStr = "* AND -origin";
            Query q = queryParser.parse(queryStr);
            queryAndDisplay(q, index);
        }
    }

    private static void addMetaFieldNames(Document doc) {
        // Add extra meta field_names that has all other field names as a value.
        StringBuilder sb = new StringBuilder();
        for (IndexableField field : doc.getFields()) {
            sb.append(field.name());
            // Use ' ' as a separator
            sb.append(' ');
        }
        System.out.println("field_names: " + sb.toString());
        doc.add(new TextField("field_names", sb.toString(), Field.Store.YES));
    }

    private static void queryAndDisplay(Query q, Directory index) throws IOException {
        int maxHits = 10;
        System.out.println("Query: " + q.toString());

        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, maxHits);
        ScoreDoc[] hits = docs.scoreDocs;

        // Display
        System.out.println("Found " + hits.length + " docs.");
        for(int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document doc = searcher.doc(docId);
            System.out.println((i + 1) + ". Title=" + doc.get("title") + " author=" + doc.get("author"));
        }
    }
}
