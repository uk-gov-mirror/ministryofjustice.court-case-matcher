package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    public final CourtCaseRestClient restClient;

    public final CaseMapper caseMapper;

    public void match(Case incomingCase) {
        Optional<CourtCase> existingCase = restClient.getCourtCase(incomingCase.getBlock().getSession().getCourtCode(), incomingCase.getCaseNo()).blockOptional();
//        TODO: I think this class probably has too many responsibilities. I will implement here for now and review after to see if I can break it out into logical components
        CourtCase courtCase = existingCase
                .map(courtCaseApi1 -> caseMapper.merge(incomingCase, courtCaseApi1))
//                TODO: At this point in the flow we need to introduce a new call to offender search to get the crn etc
                .orElse(caseMapper.newFromCase(incomingCase));
        restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase);
    }
}
