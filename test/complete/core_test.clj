(ns complete.core-test
  (:use complete.core clojure.test))

(deftest completions-test
  (is (= '("alength" "alias" "all-ns" "alter" "alter-meta!" "alter-var-root")
         (completions "al" 'clojure.core)))

  (is (= '("jio/make-input-stream" "jio/make-output-stream" "jio/make-parents" "jio/make-reader" "jio/make-writer")
         (completions "jio/make" 'clojure.core)))

  (is (= '("clojure.core/alter" "clojure.core/alter-meta!" "clojure.core/alter-var-root")
         (completions "clojure.core/alt" 'clojure.core)))

  (is (= '("complete.core" "complete.core-test")
         (completions "complete.core")))
  
  (is (some #{"clojure.core.Vec"} (completions "clojure.core")))
  
  (is (= '("System/out")
         (completions "System/o")))
  
  (is (= '("java.lang.System/out")
         (completions "java.lang.System/out")))
  
  (is (some #{"String/valueOf"} (completions "String/")))
  
  (is (not (some #{"String/indexOf" ".indexOf"} (completions "String/")))))

;; shouldn't complete be able to complete on instance methods too ? 
;; but instance methods of what ? 
;; - An indexed list of available instance methods
;;    (introspecting all the accessible classpath ?)
;; - Classes who are accessible from the namespace ?
;; - Classes who are accessible from the namespace + a set of collected
;;   additional classes (e.g. found type hints, return results, etc.)

