package eventichs.api.eventichs_api.DAO

import eventichs.api.eventichs_api.Exceptions.ConflitAvecUneRessourceExistanteException
import eventichs.api.eventichs_api.Mapper.InvitationOrganisationMapper
import eventichs.api.eventichs_api.Modèle.InvitationOrganisation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class InvitationOrganisationDAOImplMémoire(val db: JdbcTemplate): InvitationOrganisationDAO {
    override fun chercherTous(): List<InvitationOrganisation> =
        db.query("select * from Invitation_organisation JOIN utilisateur on idDestinataire = utilisateur.id JOIN organisation on idOrganisation = organisation.id", InvitationOrganisationMapper())

    override fun chercherParID(id: Int): InvitationOrganisation? =
        db.queryForObject("select * from Invitation_organisation where id = $id", InvitationOrganisationMapper())

    override fun ajouter(element: InvitationOrganisation): InvitationOrganisation? {
        val listeInvitations = chercherParOrganisation(element.Organisation.id)
        //Comment vérifié qu'une invitation n'existe pas qui a le même idDestinataire et le même idOrganisation.
        // if idDestinataire /= null && /= element.idDestinataire && idOrganisation /= element.idOrganisation
        for (invitation : InvitationOrganisation in listeInvitations) {
            if (invitation.Organisation != null) {
                if (invitation.Utilisateur == element.Utilisateur) {
                        throw ConflitAvecUneRessourceExistanteException(" Il y existe déjà une invitation à l'organisation ${element.Organisation.nomOrganisation} assigné au participant ${element.Utilisateur?.nom} inscrit au service ")
                }
            }
        }

        db.update("INSERT INTO invitation_organisation (idDestinataire, idOrganisation) SELECT ${element.Utilisateur?.id}, ${element.Organisation.id} FROM DUAL WHERE NOT EXISTS (SELECT * FROM invitation_organisation WHERE idDestinataire=${element.Utilisateur?.id} AND idOrganisation=${element.Organisation.id} LIMIT 1)")
/*
        db.update(
            "insert into Invitation_organisation values ( ?, ?, ? , ?, ?)",
            element.id,
            element.Utilisateur?.id,
            element.Organisation.id,
            element.jeton,
            element.status)
*/
        //Query pour obtenir l'id de la nouvelle invitation dernièrement créé.
        val id = db.queryForObject<Int>("SELECT @lid:=LAST_INSERT_ID(); ")
        return chercherParID(id)
    }

    override fun modifier(element: InvitationOrganisation): InvitationOrganisation? {
        return super.modifier(element)
    }

    override fun supprimerParID(id: Int): InvitationOrganisation? {
        val invitation = chercherParID(id)
        db.update("delete from Invitation_organisation where id = $id")
        return invitation
    }

    override fun chercherParOrganisation(idOrganisation: Int) : List<InvitationOrganisation> {
        //val organisation : Organisation = db.queryForObject("select * from Organisation where idOrganisation = $idOrganisation") ?: throw ConflitAvecUneRessourceExistanteException("Cette organisation n'existe pas dans le service")
        return db.query("select * from Invitation_organisation where idOrganisation = $idOrganisation", InvitationOrganisationMapper())
    }

    override fun chercherParParticipant(idParticipant: Int): List<InvitationOrganisation> =
        db.query("select * from Invitation_organisation where idDestinataire = $idParticipant", InvitationOrganisationMapper())

    override fun changerStatus(idInvitationOrganisation: Int, status: String): InvitationOrganisation? {
        db.update("update Invitation_organisation set `status` = ? where id = ?",status, idInvitationOrganisation)
        return chercherParID(idInvitationOrganisation)
    }


    //Insertion d'une invitation ayant aucun destinataire, le bon id Organisation et le status 'envoyé'
    //Un select pour obtenir l'id de l'invitation dernièrement créé.
    //Un update sur l'invitation dernièrement créé grace à l'id pour y ajouter un jeton de 8 charactères alléatoire.
    override fun crééJeton(idOrganisation: Int): InvitationOrganisation? {
        db.update(
            "INSERT INTO Invitation_organisation (idDestinataire, idOrganisation, status) VALUES (null, $idOrganisation,'généré'); ")
        val id = db.queryForObject<Int>("SELECT @lid:=LAST_INSERT_ID(); ")
        db.update("update invitation_organisation set jeton=concat( " +
                    "substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand($id)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand(@seed)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand(@seed)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand(@seed)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand(@seed)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand(@seed)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed:=round(rand(@seed)*4294967296))*36+1, 1)," +
                    "  substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', rand(@seed)*36+1, 1)" +
                    ")" +
                    "where id=$id;")
        return chercherParID(id)
    }

    override fun saisirJeton(jeton: String, idUtilisateur: Int): InvitationOrganisation? {
        val invitation : InvitationOrganisation? = db.queryForObject("select * from invitation_organisation where jeton = ?", InvitationOrganisationMapper(),jeton)
        val id : Int? = invitation?.id
        db.update("update Invitation_organisation set idDestinataire = $idUtilisateur, status = 'accepté' where id = $id")

        //appeller fonction d'ajouter organisation membre ici.

        return chercherParID(invitation!!.id)
    }
}