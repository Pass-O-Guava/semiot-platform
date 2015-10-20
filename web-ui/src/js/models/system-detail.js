"use strict";

import { EventEmitter } from 'events';
import moment from 'moment';

export default function(
    $http,
    commonUtils,
    sparqlUtils,
    tsdbUtils,
    CONFIG
) {

    class Instance extends EventEmitter {
        constructor() {
            super();
        }
        fetchSystemName(uri, callback) {
            return sparqlUtils.executeQuery(CONFIG.SPARQL.queries.getSystemName.format(uri), callback);
        }
        fetchSystemEndpoint(uri, callback) {
            return sparqlUtils.executeQuery(CONFIG.SPARQL.queries.getSystemEndpoint.format(uri), callback);
        }
        fetchArchiveObservations(metric, range) {
            return tsdbUtils.getArchiveObservations(metric, range);
        }
        fetchArchiveStates(metric, range) {
            return tsdbUtils.getArchiveStates(metric, range);
        }
        fetchLastTestimonials(metric) {
            return tsdbUtils.getLastObservation(metric);
        }
    }

    return new Instance();
}
