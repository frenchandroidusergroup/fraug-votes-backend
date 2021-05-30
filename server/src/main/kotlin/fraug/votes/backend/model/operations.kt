package fraug.votes.backend.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.runBlocking
import org.mindrot.jbcrypt.BCrypt
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class RootQuery : Query {
  fun questions(): List<Question> {
    return Firestore.getQuestions().map {
      Question(it.id, it.choiceIds)
    }
  }
}

@Component
class RootMutation : Mutation {
  fun setup(password: String): Boolean {
    val existingPasswordHash = Firestore.getPasswordHash()
    check(existingPasswordHash == null) {
      "This instance has already been set up"
    }

    val hash = BCrypt.hashpw(password, BCrypt.gensalt())
    Firestore.setPasswordHash(hash)

    return true
  }

  fun admin(password: String): Admin {
    check(BCrypt.checkpw(password, Firestore.getPasswordHash())) {
      "wrong password"
    }

    return Admin()
  }

  /**
   * register a vote for the active question
   *
   * @param userId an identifier for a user. A given user can change their votes by voting multiple times
   * @param choiceId a choiceId. No validation is made. If an invalide choiceId is entered, the vote will
   * be ignored
   *
   * @return always returns true. Failures will be returned as errors
   */
  fun vote(userId: String, choiceId: String): Boolean {
    Firestore.upsertVote(userId, choiceId)
    return true
  }
}


@Component
class RootSubscription : Subscription {
  @OptIn(FlowPreview::class)
  @GraphQLDescription("Listens to votes for a given question")
  fun votes(questionId: String): Publisher<List<VoteCount>> {
    val choiceIds = Firestore.getQuestionChoices(questionId)

    check(choiceIds != null)

    return Firestore.listenToVotes(questionId).map { votes ->
      choiceIds.map { choiceId ->
        VoteCount(choiceId, votes.count { vote -> vote == choiceId })
      } + VoteCount(null, votes.count { vote -> !choiceIds.contains(vote) })
    }.debounce(100)
      .asPublisher()
  }
}

@GraphQLDescription("The root for mutation that requires the admin password.")
class Admin {
  @GraphQLDescription("Deletes the admin password. Always true.")
  fun deletePassword(): Boolean {
    Firestore.deletePasswordHash()
    return true
  }

  @GraphQLDescription("Create a new question. Always true.")
  fun createQuestion(
    @GraphQLDescription("a unique id for this question")
    questionId: String,
    @GraphQLDescription(" a list of choices. For a given question, all choices must be different. This is typically the identifier that users will input in the Twitch chat with `!benjamin`")
    choiceIds: List<String>
  ): Boolean {
    check(choiceIds.size >= 2) {
      "A question must have at least two choiceIds"
    }
    Firestore.createQuestion(questionId, choiceIds)
    return true
  }

  @GraphQLDescription("Activate the given question. Votes will use this question from now on")
  fun activateQuestion(questionId: String?): Boolean {
    Firestore.setActiveQuestion(questionId)
    return true
  }

  /**
   *
   *
   * @return always returns true. Failures will be returned as errors
   */
  @GraphQLDescription("Delete the given question. Always true.")
  fun deleteQuestion(questionId: String): Boolean {
    Firestore.deleteQuestion(questionId)
    return true
  }
}

@GraphQLDescription("The number of votes for a given choice.")
class VoteCount(
  @GraphQLDescription("The choiceId. null for votes with an invalid choiceId.")
  val choiceId: String?,
  val count: Int
)

class Question(val id: String, val choiceIds: List<String>) {
  fun votes(): List<VoteCount> {
    return runBlocking {
      Firestore.listenToVotes(id).first().let { votes ->
        choiceIds.map { choiceId ->
          VoteCount(choiceId, votes.count { vote -> vote == choiceId })
        } + VoteCount(null, votes.count { vote -> !choiceIds.contains(vote) })
      }
    }
  }
}
