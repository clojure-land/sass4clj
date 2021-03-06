(ns sass4clj.main
  (:require [sass4clj.api :as api]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.edn :as edn]))

(def cli-opts
  [["-h" "--help"]
   ["-a" "--auto" "Enable file watcher"]
   ["-t" "--target-path TARGET" "The path where CSS files are written to"
    :default "target"]
   ["-s" "--source-map" "Enable source-maps for compiled CSS"]
   [nil "--output-style STYLE" "Select output style, available styles are nested, compact, expanded and compressed"
    :parse-fn (fn [s] (keyword s))
    :validate [#{:nested :compact :expanded :compressed} "Invalid value"]]
   ["-v" "--verbosity LEVEL" "Set verbosity level, valid values are 1 and 2."
    :parse-fn (fn [s]
                (Integer/parseInt s))
    :validate [#{1 2} "Must be 1 or 2"]]
   [nil "--source-paths PATHS" "List of SASS source paths, comma separated"
    :default ["src"]
    :parse-fn (fn [x]
                (str/split x #","))]
   ["-i" "--inputs PATHS" "List of SASS main file paths, relative to source-path, comma separated"
    :parse-fn (fn [x]
                (str/split x #","))]
   ["-c" "--config PATH" "EDN file to read config options from"]])

(defn help-text [options-summary]
(str "{sass} CSS compiler.

Usage: program-name [options]

If inputs option if given, compiles each file to same relative path in
target-path. If inputs is not used, finds each `.scss` or `.sass` file,
not starting with prefix `_`, in source-paths.

If you are seeing SLF4J warnings, check https://github.com/Deraen/sass4clj#log-configuration

Options:
" options-summary
"

Config file options are merged over the default options, before CLI options."))

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-opts :no-defaults true)
        {:keys [help]} options
        config-file (if (:config options)
                      (edn/read-string (slurp (io/file (:config options)))))
        options (merge (cli/get-default-options cli-opts)
                       config-file
                       (dissoc options :config))]
    (cond
      errors (println (str/join "\n" errors))
      help (println (help-text summary))
      :else (api/build options))))
