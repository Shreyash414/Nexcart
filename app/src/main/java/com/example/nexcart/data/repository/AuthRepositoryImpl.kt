package com.example.nexcart.data.repository

import com.example.nexcart.domain.model.AuthResult
import com.example.nexcart.domain.model.User
import com.example.nexcart.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val user = getUserFromFirestore(result.user!!.uid) ?: User(
                uid = result.user!!.uid,
                email = result.user!!.email,
                name = result.user!!.displayName,
                photoUrl = result.user!!.photoUrl.toString()
            )
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            var user = getUserFromFirestore(result.user!!.uid)
            if (user == null) {
                user = User(
                    uid = result.user!!.uid,
                    email = result.user!!.email,
                    name = result.user!!.displayName,
                    photoUrl = result.user!!.photoUrl.toString(),
                    seller = false
                )
                saveUserToFirestore(user)
            }
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success(User(uid = "", email = "", name = "", photoUrl = ""))
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    override suspend fun signUp(name: String, email: String, pass: String, isSeller: Boolean): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            result.user!!.updateProfile(profileUpdates).await()
            val user = User(
                uid = result.user!!.uid,
                email = result.user!!.email,
                name = name,
                photoUrl = null,
                seller = isSeller
            )
            saveUserToFirestore(user)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getAuthState(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                launch {
                    val user = getUserFromFirestore(firebaseUser.uid) ?: User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        name = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl.toString()
                    )
                    trySend(user)
                }
            }
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return getUserFromFirestore(uid)
    }

    override suspend fun getUserById(uid: String): User? {
        return getUserFromFirestore(uid)
    }

    private suspend fun saveUserToFirestore(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }

    private suspend fun getUserFromFirestore(uid: String): User? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
