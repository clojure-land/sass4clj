(ns leiningen.sass4clj
  (:require [leiningen.help]
            [leiningen.core.eval :as leval]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.help :as help]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [sass4clj.version :refer [+version+]]))

(def sass4j-profile {:dependencies [['watchtower "0.1.1"]
                                    ['deraen/sass4clj +version+]]})

; From lein-cljsbuild
(defn- eval-in-project [project form requires]
  (leval/eval-in-project
    project
    ; Without an explicit exit, the in-project subprocess seems to just hang for
    ; around 30 seconds before exiting.  I don't fully understand why...
    `(try
       ~form
       (System/exit 0)
       (catch Exception e#
         (do
           (.printStackTrace e#)
           (System/exit 1))))
    requires))

(defn- run-compiler
  "Run the sasscss compiler."
  [project options]
  (eval-in-project
    (project/merge-profiles project [sass4j-profile])
    `(cond-> (sass4clj.api/build ~options)
       ~(:auto options) deref)
    '(require 'sass4clj.api 'watchtower.core)))

;; For docstrings

(defn- once
  "Compile sass files once."
  [project]
  nil)

(defn- auto
  "Compile sass files, then watch for changes and recompile until interrupted."
  [project]
  nil)

(defn sass4clj
  "SASS CSS compiler.

For each `.sass` or `.scss` file not starting with `_` in source-paths creates equivalent `.css` file.
For example to create file `{target-path}/public/css/style.css` your sass
code should be at path `{source-path}/public/css/style.scss`.

If you are seeing SLF4J warnings, check https://github.com/Deraen/sass4clj#log-configuration

Options should be provided using `:sass` key in project map.

Available options:
:target-path          The path where CSS files are written to.
:source-paths         Collection of paths where SASS files are read from.
:output-style         Possible types are :nested, :compact, :expanded and :compressed.
:verbosity            Set verbosity level, valid values are 1 and 2.
:source-map           Enable source-maps.

Other options are passed as is to sass4clj.

Command arguments:
Add `:debug` as subtask argument to enable debugging output."
  {:help-arglists '([once auto])
   :subtasks      [#'once #'auto]}
  ([project]
   (println (help/help-for "sass4clj"))
   (main/abort))
  ([project subtask & args]
   (let [args (set args)
         config (cond-> (:sass project)
                  (contains? args ":debug") (assoc :verbosity 2))]
     (case subtask
       "once" (run-compiler project config)
       "auto" (run-compiler project (assoc config :auto true))
       "help" (println (help/help-for "sass4clj"))
       (main/warn "Unknown task.")))))