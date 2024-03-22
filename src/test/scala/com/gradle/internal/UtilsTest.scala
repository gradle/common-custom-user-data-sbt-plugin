package com.gradle.internal

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.net.URI

class UtilsTest extends AnyFlatSpec
  with Matchers
  with TableDrivenPropertyChecks {

  "Utils" should "construct a repo URI from a git URL" in {
    forEvery(webRepoUriArgumentsTable) { (repositoryUri, expectedWebRepoUri) =>
      assertResult(Some(URI.create(expectedWebRepoUri))) (Utils.toWebRepoUri(repositoryUri))
    }
  }

  private lazy val webRepoUriArgumentsTable = Table((
    "remote repository", "expected resolved repository"),
    (for (host <- hosts; repo <- repos) yield (replaceHost(repo._1, host), replaceHost(repo._2, host))): _*
  )

  private def replaceHost(repo: String, host: String): String = String.format(repo, host)

  private val hosts = Table(
    "host",
    "github",
    "gitlab"
  )

  private val repos = Table(
    ("remote repository", "expected resolved repository"),
    ("https://%s.com/acme-inc/my-project", "https://%s.com/acme-inc/my-project"),
    ("https://%s.com:443/acme-inc/my-project", "https://%s.com/acme-inc/my-project"),
    ("https://user:secret@%s.com/acme-inc/my-project", "https://%s.com/acme-inc/my-project"),
    ("ssh://git@%s.com/acme-inc/my-project.git", "https://%s.com/acme-inc/my-project"),
    ("ssh://git@%s.com:22/acme-inc/my-project.git", "https://%s.com/acme-inc/my-project"),
    ("git://%s.com/acme-inc/my-project.git", "https://%s.com/acme-inc/my-project"),
    ("git@%s.com/acme-inc/my-project.git", "https://%s.com/acme-inc/my-project"),
    // Enterprise repos
    ("https://%s.acme.com/acme-inc/my-project", "https://%s.acme.com/acme-inc/my-project"),
    ("git@%s.acme.com/acme-inc/my-project.git", "https://%s.acme.com/acme-inc/my-project"),
  )

}
