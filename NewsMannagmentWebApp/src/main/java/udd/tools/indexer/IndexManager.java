package udd.tools.indexer;

public class IndexManager {
	
	private static UDDIndexer indexer = new UDDIndexer(true);
	
	public static UDDIndexer getIndexer(){
		if(indexer == null){
			indexer = new UDDIndexer(true);
		}
		return indexer;
	}

}
