(ns cludje.templatefind-test
  (:use midje.sweet
        cludje.test
        cludje.system
        cludje.model
        cludje.types
        cludje.moldfind
        cludje.templatefind))

(def cog (>Model :cog {:name Str} {}))
(defn template-for-edit [model] "Edit")
(defn template-for-delete [model] "Delete")
(defn >moldfinder [] (>NSMoldFinder 'cludje.templatefind-test))
(defn >context [template-name] {:template-name template-name
                                :system {:mold-finder (>moldfinder)}})

(facts ">GenericTemplateFinder"
  (let [gf (>GenericTemplateFinder 'cludje.templatefind-test 'cludje.altnamespace)]
    (fact "instantiates"
      (satisfies? ITemplateFinder gf) => true)
    (fact "finds template"
      (fact "with string"
        (find-template gf (>context "edit-cog")) => "Edit")
      (fact "with keyword"
        (find-template gf (>context :edit-cog)) => "Edit"))
    (fact "works with another namespace"
      (find-template gf (>context :add-cog)) => "Altns add")
    (fact "Not finding returns nil"
      (find-template gf (>context :foo-bar)) => nil)
    (fact "Throws if there's no model"
      (find-template gf (>context :edit-foo)) => (throws-error))))

(defn show-cog [] "Show")
(defn show-foo [] "Foo")

(facts ">SpecificTemplateFinder"
  (let [mf (>SpecificTemplateFinder 'cludje.templatefind-test 'cludje.altnamespace)]
    (fact "instantiaties"
      (satisfies? ITemplateFinder mf) => true)
    (fact "finds template"
      (fact "with string"
        (find-template mf (>context "show-cog")) => "Show")
      (fact "with keyword"
        (find-template mf (>context :show-cog)) => "Show"))
    (fact "works with another namespace"
      (find-template mf (>context :inc-cog)) => "Altns inc cog")
    (fact "Not finding returns nil"
      (find-template mf (>context :bar-foo)) => nil)
    (fact "Runs even if there's no model"
      (find-template mf (>context :show-foo)) => "Foo")))


(facts ">CompositeTemplateFinder"
  (let [cf (>CompositeTemplateFinder 'cludje.templatefind-test 'cludje.altnamespace)]
    (fact "Finds specific template first"
      (find-template cf (>context :edit-cog)) => "Altns edit cog")
    (fact "Find generic if there's no specific"
      (find-template cf (>context :delete-cog)) => "Delete")
    (fact "Doesn't find generics in specific namespace"
      (find-template cf (>context :show-cog)) => nil)
    ))

