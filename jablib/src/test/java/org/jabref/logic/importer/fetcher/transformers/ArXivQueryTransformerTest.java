package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArXivQueryTransformerTest extends YearRangeByFilteringQueryTransformerTest<ArXivQueryTransformer> {

    @Override
    public ArXivQueryTransformer getTransformer() {
        return new ArXivQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "all:";
    }

    @Override
    public String getJournalPrefix() {
        return "jr:";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    @Test
    public void convertYearField() throws QueryNodeParseException {
        ArXivQueryTransformer transformer = getTransformer();
        String queryString = "year:2018";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = transformer.transformLuceneQuery(luceneQuery);
        assertEquals(Optional.of("2018"), query);
        assertEquals(Optional.of(2018), transformer.getStartYear());
        assertEquals(Optional.of(2018), transformer.getEndYear());
    }
}
