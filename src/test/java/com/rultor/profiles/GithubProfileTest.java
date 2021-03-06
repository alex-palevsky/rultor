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
package com.rultor.profiles;

import com.google.common.base.Joiner;
import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Repo;
import com.jcabi.github.mock.MkGithub;
import com.jcabi.matchers.XhtmlMatchers;
import com.rultor.spi.Profile;
import java.io.IOException;
import javax.json.Json;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Tests for ${@link GithubProfile}.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 * @checkstyle MultipleStringLiteralsCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class GithubProfileTest {

    /**
     * GithubProfile can fetch a YAML config.
     * @throws Exception In case of error.
     */
    @Test
    public void fetchesYamlConfig() throws Exception {
        final Repo repo = GithubProfileTest.repo(
            Joiner.on('\n').join(
                "assets:",
                "  test.xml: jeff/test1#test.xml",
                "  beta: jeff/test1#test.xml",
                "merge:",
                "  script: hello!"
            )
        );
        final String yaml = "friends:\n  - jeff/test2";
        repo.github()
            .repos()
            .get(new Coordinates.Simple("jeff/test1"))
            .contents().create(
                Json.createObjectBuilder()
                    .add("path", ".rultor.yml")
                    .add("message", "rultor config")
                    .add("content", Base64.encodeBase64String(yaml.getBytes()))
                    .build()
            );
        final Profile profile = new GithubProfile(repo);
        MatcherAssert.assertThat(
            profile.read(),
            XhtmlMatchers.hasXPaths(
                "/p/entry[@key='merge']/entry[@key='script']",
                "/p/entry[@key='assets']/entry[@key='test.xml']",
                "/p/entry[@key='assets']/entry[@key='beta']"
            )
        );
        MatcherAssert.assertThat(
            profile.assets(),
            Matchers.hasEntry(
                Matchers.equalTo("test.xml"),
                Matchers.notNullValue()
            )
        );
    }

    /**
     * GithubProfile can throw when YAML is broken.
     * @throws Exception In case of error.
     */
    @Test(expected = Profile.ConfigException.class)
    public void throwsWhenYamlIsBroken() throws Exception {
        new GithubProfile(GithubProfileTest.repo("&*(fds:[[\nfd\n")).read();
    }

    /**
     * GithubProfile can throw if asset is misconfigured.
     * @throws Exception In case of error.
     * @since 1.33
     */
    @Test(expected = Profile.ConfigException.class)
    public void throwsWhenAssetIsMisconfigured() throws Exception {
        final Repo repo = GithubProfileTest.repo(
            Joiner.on('\n').join(
                "assets: ",
                "  something.xml: -invalid.user.name/test1#test.xml"
            )
        );
        new GithubProfile(repo).assets();
    }

    /**
     * GithubProfile can throw when rultor.yml is absent.
     * @throws Exception In case of error.
     */
    @Test(expected = Profile.ConfigException.class)
    public void throwsWhenRultorConfigIsAbsent() throws Exception {
        final Repo repo = GithubProfileTest.repo(
            Joiner.on('\n').join(
                "assets:   ",
                "  something.xml: jeff/test2#.rultor.yml"
            )
        );
        new GithubProfile(repo).assets();
    }

    /**
     * GithubProfile can throw when friend is not defined.
     * @throws Exception In case of error.
     */
    @Test(expected = Profile.ConfigException.class)
    public void throwsWhenFriendNotDefined() throws Exception {
        final Repo repo = GithubProfileTest.repo(
            Joiner.on('\n').join(
                "assets:    ",
                "  a.xml: jeff/test1#test.xml"
            )
        );
        new GithubProfile(repo).assets();
    }

    /**
     * GithubProfile should throw a ConfigException if some asset file doesn't
     * exist.
     * @throws Exception In case of error.
     */
    @Test(expected = Profile.ConfigException.class)
    public void testAssetNotFound() throws Exception {
        final Repo repo = GithubProfileTest.repo(
            Joiner.on('\n').join(
                "assets:",
                "  test.xml: jeff/test1#test.xmls",
                "merge:",
                "  script: hello!"
            )
        );
        final String yaml = "friends:\n  - jeff/test2";
        repo.github()
            .repos()
            .get(new Coordinates.Simple("jeff/test1"))
            .contents().create(
                Json.createObjectBuilder()
                    .add("path", ".rultor.yml")
                    .add("message", "rultor config")
                    .add("content", Base64.encodeBase64String(yaml.getBytes()))
                    .build()
            );
        final Profile profile = new GithubProfile(repo);
        profile.assets();
    }

    /**
     * Make a repo with YAML inside.
     * @param yaml YAML config
     * @return Repo
     * @throws IOException If fails
     */
    private static Repo repo(final String yaml) throws IOException {
        final Github github = new MkGithub("jeff");
        github.repos()
            .create(Json.createObjectBuilder().add("name", "test1").build())
            .contents()
            .create(
                Json.createObjectBuilder()
                    .add("path", "test.xml")
                    .add("message", "just test msg")
                    .add("content", Base64.encodeBase64String("hey".getBytes()))
                    .build()
            );
        final Repo repo = github.repos().create(
            Json.createObjectBuilder().add("name", "test2").build()
        );
        repo.contents().create(
            Json.createObjectBuilder()
                .add("path", ".rultor.yml")
                .add("message", "just test")
                .add("content", Base64.encodeBase64String(yaml.getBytes()))
                .build()
        );
        return repo;
    }
}
