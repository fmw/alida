================================================================
Alida: a crawling, scraping and indexing tool written in Clojure
================================================================

The Alida project was started as companion code to my talk at
`EuroClojure 2012`_ on the topic of "Building a search engine with
Clojure". The goal of this application is to provide the back-end for
a simple search engine, while the front-end (i.e. the part that
performs the actual searches and displays the results to the visitor)
is separate. Alida will provide the following functionality:

1. Data retrieval (i.e. web crawling).
2. Storage (storing the crawled pages).
3. Scraping (extracting interesting bits and pieces from documents).
4. Indexing (storing the end result in a searchable index).

The application depends on several external libraries and
applications, including `Apache CouchDB`_ to store the crawled pages,
`Apache Lucene`_ for indexing, `clj-http`_ as a wrapper around the
`Apache HttpClient`_, `Clutch`_ as a CouchDB library and both
`Enlive`_ and `Jsoup`_ for scraping data from the retrieved
documents. As an experimental project Alida isn't designed for
immediate production use. Some technology choices reflect that. Apache
CouchDB, for example, isn't the most obvious choice for storing
crawled pages. For large scale document storage something like `HDFS`_
would be more efficient, but CouchDB is easier to set up in a small,
experimental setting. If you need something more mature, I'd suggest
looking at `Apache Nutch`_, which also includes a web crawler. That
being said, one of the goals for Alida is to be able to power a
real-world search engine project.


Questions?
----------

Don't hesitate to contact me if you have any questions or
feedback. You can email me at fmw@vixu.com.

About me
--------

My name is Filip de Waard. As the founder of `Vixu.com`_ I write
Clojure code for a living. The main focus of `Vixu.com`_ is providing
website-management software as a service. Under the hood we use the
free, open source `Vix`_ application to power the service. My company
is also working on a product search application written in Clojure.


License
-------

Copyright 2012, F.M. de Waard / `Vixu.com`_.
All code is covered by the `Apache License, version 2.0`_.

.. _`EuroClojure 2012`: http://euroclojure.com/2012/
.. _`Apache CouchDB`: http://couchdb.apache.org/
.. _`Apache Lucene`: http://lucene.apache.org/core/
.. _`clj-http`: https://github.comdakrone/clj-http
.. _`Apache HttpClient`: http://hc.apache.org/httpcomponents-client-ga/index.html
.. _`Clutch`: https://github.com/clojure-clutch/clutch
.. _`Enlive`: https://github.com/cgrand/enlive
.. _`Jsoup`: http://jsoup.org/
.. _`HDFS`: http://hadoop.apache.org/hdfs/
.. _`Apache Nutch`: http://nutch.apache.org/
.. _`Vixu.com`: http://www.vixu.com/
.. _`Vix`: https://github.com/fmw/vix
.. _`Apache License, version 2.0`: http://www.apache.org/licenses/LICENSE-2.0.html
