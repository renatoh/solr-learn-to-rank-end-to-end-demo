#create features in default feature store
curl -k -X POST -H 'Content-type:application/json'  --data-binary @solr-features.json https://localhost:8983/solr/master_powertools_Product_default/schema/feature-store/default

#remove feattures from default feature store
curl -k -XDELETE https://localhost:8983/solr/master_powertools_Product_default/schema/feature-store/_DEFAULT_

#see featurue-stroe
http://localhost:8983/solr/techproducts/schema/feature-store/commonFeatureStore

