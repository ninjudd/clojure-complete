(ns complete.core
  (:require [clojure.main])
  (:import [java.util.jar JarFile]
           [java.io File]
           [java.lang.reflect Member]
           [java.util.jar JarEntry]))

;; Code adapted from swank-clojure (http://github.com/jochu/swank-clojure)

(defn namespaces
  "Returns a list of potential namespace completions for a given namespace"
  [ns]
  (map name (concat (map ns-name (all-ns)) (keys (ns-aliases ns)))))

(defn ns-public-vars
  "Returns a list of potential public var name completions for a given namespace"
  [ns]
  (map name (keys (ns-publics ns))))

(defn ns-vars
  "Returns a list of all potential var name completions for a given namespace"
  [ns]
  (for [[sym val] (ns-map ns) :when (var? val)]
    (name sym)))

(defn ns-classes
  "Returns a list of potential class name completions for a given namespace"
  [ns]
  (map name (keys (ns-imports ns))))

(def special-forms
  (map name '[def if do let quote var fn loop recur throw try monitor-enter monitor-exit dot new set!]))

(defn- static? [#^java.lang.reflect.Member member]
  (java.lang.reflect.Modifier/isStatic (.getModifiers member)))

(defn ns-java-methods
  "Returns a list of potential java method name completions for a given namespace"
  [ns]
  (for [class (vals (ns-imports ns)) method (.getMethods ^Class class) :when (static? method)]
    (str "." (.getName ^Member method))))

(defn static-members
  "Returns a list of potential static members for a given class"
  [^Class class]
  (for [member (concat (.getMethods class) (.getDeclaredFields class)) :when (static? member)]
    (.getName ^Member member)))

(defn path-files [^String path]
  (cond (.endsWith path "/*")
        (for [^File jar (.listFiles (File. path)) :when (.endsWith ^String (.getName jar) ".jar")
              file (path-files (.getPath jar))]
          file)

        (.endsWith path ".jar")
        (try (for [^JarEntry entry (enumeration-seq (.entries (JarFile. path)))]
               (.getName entry))
             (catch Exception e))

        :else
        (for [^File file (file-seq (File. path))]
          (.replace ^String (.getPath file) path ""))))

(def classfiles
  (for [prop (filter #(System/getProperty %1) ["sun.boot.class.path" "java.ext.dirs" "java.class.path"])
        path (.split (System/getProperty prop) File/pathSeparator)
        ^String file (path-files path) :when (and (.endsWith file ".class") (not (.contains file "__")))]
    file))

(defn- classname [^String file]
  (.. file (replace ".class" "") (replace File/separator ".")))

(def top-level-classes
  (future
    (doall
     (for [file classfiles :when (re-find #"^[^\$]+\.class" file)]
       (classname file)))))

(def nested-classes
  (future
    (doall
     (for [file classfiles :when (re-find #"^[^\$]+(\$[^\d]\w*)+\.class" file)]
       (classname file)))))

(defn resolve-class [sym]
  (try (let [val (resolve sym)]
         (when (class? val) val))
       (catch Exception e
         (when (not= ClassNotFoundException
                     (class (clojure.main/repl-exception e)))
           (throw e)))))

(defmulti potential-completions
  (fn [^String prefix ns]
    (cond (.contains prefix "/") :scoped
          (.contains prefix ".") :class
          :else                  :var)))

(defmethod potential-completions :scoped
  [^String prefix ns]
  (when-let [prefix-scope (first (.split prefix "/"))]
    (let [scope (symbol prefix-scope)]
      (map #(str scope "/" %)
           (if-let [class (resolve-class scope)]
             (static-members class)
             (when-let [ns (or (find-ns scope) (scope (ns-aliases ns)))]
               (ns-public-vars ns)))))))

(defmethod potential-completions :class
  [^String prefix ns]
  (concat (namespaces ns)
          (if (.contains prefix "$")
            @nested-classes
            @top-level-classes)))

(defmethod potential-completions :var
  [_ ns]
  (concat special-forms
          (namespaces ns)
          (ns-vars ns)
          (ns-classes ns)
          (ns-java-methods ns)))

(defn completions
  "Return a sequence of matching completions given a prefix string and an optional current namespace."
  ([prefix] (completions prefix *ns*))
  ([^String prefix ns]
     (sort (for [^String completion (potential-completions prefix ns) :when (.startsWith completion prefix)]
             completion))))
