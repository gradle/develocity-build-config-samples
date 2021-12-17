(ns packaging)

(defn hello-world
  []
  (println "Hello World!!!???"))

(defmacro nestfn [n & body]
  (if (> n 0)
    `(fn [] (nestfn ~(- n 1) ~@body))
    body))

(def myf (nestfn 20 "body"))