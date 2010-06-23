(ns complete
  (:use [useful :only [defm]])
  (:import [java.util.jar JarFile]
           [java.io File]))

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

(defn- static? [#^java.lang.reflect.Member member]
  (java.lang.reflect.Modifier/isStatic (.getModifiers member)))

(defn ns-java-methods
  "Returns a list of potential java method name completions for a given namespace"
  [ns]
  (for [class (vals (ns-imports ns)) method (.getMethods class) :when (static? method)]
    (str "." (.getName method))))

(defn static-members
  "Returns a list of potential static members for a given class"
  [class]
  (for [member (concat (.getMethods class) (.getDeclaredFields class)) :when (static? member)]
    (.getName member)))

(defn url-files [url]
  (let [path (.getFile url)]
    (if (.endsWith path ".jar")
      (for [entry (enumeration-seq (.entries (JarFile. path)))]
        (.getName entry))
      (for [file (file-seq (File. path))]
        (.replace (.getPath file) path "")))))

(defn classfiles []
  (for [url  (.getURLs (.getClassLoader clojure.lang.RT))
        file (url-files url) :when (and (.endsWith file ".class") (not (.contains file "__")))]
    file))

(defn- classname [file]
  (.. file (replace File/separator ".") (replace ".class" "")))

(defm top-level-classes []
  (for [file (classfiles) :when (re-find #"^[^\$]+\.class" file)]
    (classname file)))

(defm nested-classes []
  (for [file (classfiles) :when (re-find #"^[^\$]+(\$[^\d]\w*)+\.class" file)]
    (classname file)))

(defn resolve-class [sym]
  (try (let [val (resolve sym)]
         (when (class? val) val))
       (catch ClassNotFoundException e)))

(defmulti potential-completions
  (fn [prefix ns]
    (cond (.contains prefix "/") :scoped
          (.contains prefix ".") :class
          :else                  :var)))

(defmethod potential-completions :scoped
  [prefix ns]
  (let [scope (symbol (first (.split prefix "/")))]
    (map #(str scope "/" %)
         (if-let [class (resolve-class scope)]
           (static-members class)
           (when-let [ns (or (find-ns scope) (scope (ns-aliases ns)))]
             (ns-public-vars ns))))))

(defmethod potential-completions :class
  [prefix ns]
  (concat (namespaces ns)
          (if (.contains prefix "$")
            (nested-classes)
            (top-level-classes))))

(defmethod potential-completions :var
  [_ ns]
  (concat (namespaces ns)
          (ns-vars ns)
          (ns-classes ns)
          (ns-java-methods ns)))

(defn completions
  "Return a sequence of matching completions given a prefix string and an optional current namespace."
  ([prefix] (completions prefix *ns*))
  ([prefix ns]
     (for [completion (potential-completions prefix ns) :when (.startsWith completion prefix)]
       completion)))
