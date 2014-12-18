// Mixin the utilities
def script = new GroovyScriptEngine( '.' ).with {
    loadScriptByName( 'graph-base.groovy' )
}
this.metaClass.mixin script

// Run this script with groovy golr-dump.groovy graphLocation outputLocation

init args[0]

def query = """
MATCH (disease:disease)<-[:hasSubject]-(association)-[:hasObject]->(phenotype:Phenotype)
RETURN
'DiseasePhenotypeAssociation' as document_category,
disease.fragment as bioentity,
phenotype.fragment as annotation_class,
disease.label as bioentity_label,
phenotype.label as annotation_class_label,
id(phenotype) as phenotype_id
"""

result = engine.execute query
file = new File(args[1])
file.append('[')
while (result.iterator().hasNext()) {
    propMap = result.iterator().next()
    def doc = new Expando()
    doc.id = UUID.randomUUID().toString()
    result.columns().each { propName ->
        if (propName == 'phenotype_id') {
            (superClasses, superClassLabels) = getSuperclasses(propMap.get(propName))
            doc.setProperty('isa_partof_closure', superClasses)
            doc.setProperty('isa_partof_closure_label', superClassLabels)
        } else {
            doc.setProperty(propName, propMap.get(propName))
        }

    }
    def builder = new groovy.json.JsonBuilder(doc)
    file.append(builder.toString())

    if (result.iterator().hasNext()) {
        file.append(',')
    }
}
file.append(']')

shutdown()
