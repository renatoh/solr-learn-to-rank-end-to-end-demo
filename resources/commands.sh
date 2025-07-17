#create features in default feature store
curl -k -X POST -H 'Content-type:application/json'  --data-binary @solr-features.json https://localhost:8983/solr/master_powertools_Product_default/schema/feature-store/default

#remove feattures from default feature store
curl -k -XDELETE https://localhost:8983/solr/master_powertools_Product_default/schema/feature-store/_DEFAULT_

#see featurue-stroe
https://localhost:8983/solr/master_powertools_Product_default/schema/feature-store/commonFeatureStore


#modle upload
curl -k -XPUT 'https://localhost:8983/solr/master_powertools_Product_default/schema/model-store' --data-binary "lightgbm.json" -H 'Content-type:application/json'



https://localhost:8983/solr/master_powertools_Product_default/select?defType=dismax&indent=true&q.op=OR&q=drill&qf=brand_string_mv%20name_text_en%20manufacturerName_text%20categoryName_text_en_mv%20description_text_en&rows=50&useParams=


https://localhost:8983/solr/master_powertools_Product_default/select?defType=dismax&fl=code_string priceValue_usd_double score&indent=true&q.op=OR&q=drill&qf=brand_string_mv%20name_text_en%20manufacturerName_text%20categoryName_text_en_mv%20description_text_en&rows=200&rq={!ltr model=multipleadditivetreesmodel  efi.text=drill reRankDocs=100}


 curl -XPUT 'http://localhost:8983/solr/techproducts/schema/text-to-vector-model-store' --data-binary "@huggingFaceModelConfig.json" -H 'Content-type:application/json'
 
 curl -XPUT 'http://localhost:8983/solr/techproducts/schema/text-to-vector-model-store' --data-binary "@huggingFaceModelConfig.json" -H 'Content-type:application/json'


 curl -k -XDELETE http://localhost:8983/solr/techproducts/schema/text-to-vector-model-store/customLocal
