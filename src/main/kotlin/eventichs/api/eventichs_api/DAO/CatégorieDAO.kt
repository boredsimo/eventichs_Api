package eventichs.api.eventichs_api.DAO

import eventichs.api.eventichs_api.Modèle.Catégorie

interface CatégorieDAO: DAO<Catégorie> {

    override fun chercherTous(): List<Catégorie>
    override fun chercherParID(id: Int): Catégorie?
    override fun modifier(element: Catégorie): Catégorie?
    override fun ajouter(element: Catégorie): Catégorie?
    override fun supprimerParID(id: Int): Catégorie?

    fun validerUtilisateur(code_util : String) : Boolean
}