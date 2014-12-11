;
; Copyright © 2014 Courage Labs
;
; This file is part of Freefrog.
;
; Freefrog is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; Freefrog is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;

;;; ### These are the core rules defined in the Holacracy Constitution ###
(ns freefrog.core-roles
  (:require [freefrog.governance :as g]))

(def lead-link
  (g/make-role
    g/lead-link-name
    (str "The Lead Link Role shall be deemed to hold the purpose of the "
         "overall Circle.")
    [g/role-assignments-domain]
    [(str "Differentiating and organizing the Circle's overall work into "
          "segmented Roles and other requisite Governance")
     (str "Assigning Partners to the Circle's Roles, monitoring fit between "
          "Partners and their Roles and offering feedback to enhance fit, "
          "and removing Partners from Roles when useful")
     (str "Allocating the Circle's available resources across its various "
          "Projects and/or Roles")
     "Assessing and defining priorities and Strategies for the Circle"
     (str "Defining and assigning metrics within the Circle that provide "
          "visibility into such Circle's expression of its Purpose and "
          "enactment of its Accountabilities")]))

(def rep-link
  (g/make-role
    g/rep-link-name
    (str "Within the Super-Circle, the Rep Link Role shall be deemed to "
         "hold the purpose of the overall Circle so represented; within "
         "such Circle, the Rep Link Role's Purpose shall be: Tensions "
         "relevant to process in the Super-Circle channeled out and "
         "resolved.") nil
    [(str "Removing constraints within the Super-Circle that limit the "
          "Circle's capacity to express its Purpose or Accountabilities")
     (str "Seeking to understand Tensions conveyed by and of the Circle's "
          "Circle Members, and discerning those appropriate to channel "
          "into the Super-Circle for processing")
     (str "Providing visibility to the Super-Circle into the health and "
          "sustainability of operations within the Circle, including "
          "reporting data within the Super-Circle for any metrics or "
          "checklist items assigned to the overall Circle")]))

(def facilitator
  (g/make-role
    g/facilitator-name
    (str "Circle governance and operational practices aligned with the core "
         "rules and processes of this Constitution.") nil
    [(str "Facilitating the Circle's Governance Meetings and Tactical "
          "Meetings in alignment with the rules of this Constitution, and "
          "enforcing such rules during such meetings as-needed")
     (str "Auditing the meetings and records of the Circle's Sub-Circles "
          "to assess alignment with this Constitution, including at a "
          "minimum whenever prompted to do so by the Rep Link from a "
          "Sub-Circle, and initiating the restorative process defined in "
          "this Constitution if a Process Breakdown is discovered within "
          "a Sub-Circle")]))

(def secretary
  (g/make-role
    g/secretary-name
    (str "Stabilize the Circle's Governance over time as a steward of the "
         "Circle's formal records and record-keeping process")
    [g/governance-records-domain]
    [(str "Maintaining all records of a Circle required by this "
          "Constitution, including capturing the outputs of the Circle's "
          "governance process and Tactical Meetings, maintaining a "
          "compiled view of all Governance currently in effect for the "
          "Circle, and maintaining a list of all operational elements "
          "currently being monitored in Tactical Meetings")
     (str "Scheduling all regular and special meetings of the Circle "
          "explicitly required by this Constitution or by a Policy "
          "established by the Circle, in alignment with the terms of "
          "this Constitution and any relevant Policies of the Circle, and "
          "notifying all Core Circle Members of times and locations for "
          "meetings so scheduled")
     (str "Interpreting the acting Governance of the Circle upon request "
          "of a Circle Member as provided for in this Constitution, "
          "including ruling on matters of due process, procedure, and "
          "authority related to or granted under such Governance or this "
          "Constitution itself")]))
