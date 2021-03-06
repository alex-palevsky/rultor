/**
 * Copyright (c) 2009-2014, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.web;

import com.jcabi.matchers.XhtmlMatchers;
import com.rexsl.mock.MkServletContext;
import com.rultor.spi.Talk;
import com.rultor.spi.Talks;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.xembly.Directives;

/**
 * Test case for {@link SitemapRs}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.26
 */
public final class SitemapRsTest {

    /**
     * SitemapRs can render a list.
     * @throws Exception If some problem inside
     */
    @Test
    public void rendersListOfTalks() throws Exception {
        final SitemapRs home = new SitemapRs();
        final Talks talks = new Talks.InDir();
        talks.create("repo1", Talk.TEST_NAME);
        talks.get(Talk.TEST_NAME).modify(
            new Directives()
                .xpath("/talk")
                .add("wire").add("href").set("http://example.com").up()
                .add("github-repo").set("yegor256/rultor").up()
                .add("github-issue").set("555").up().up()
                .add("archive").add("log").attr("title", "hello, world")
                .attr("id", "a1b2c3").set("s3://test")
        );
        home.setServletContext(
            new MkServletContext().withAttr(Talks.class.getName(), talks)
        );
        MatcherAssert.assertThat(
            XhtmlMatchers.xhtml(home.xml()),
            XhtmlMatchers.hasXPath(
                "/ns1:urlset[count(ns1:url)=1]",
                "http://www.sitemaps.org/schemas/sitemap/0.9"
            )
        );
    }

}
