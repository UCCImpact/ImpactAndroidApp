package ie.ucc.bis.supportinglife.assessment.imci.ui;

import ie.ucc.bis.supportinglife.assessment.model.AbstractAssessmentModel;

/**
 * 
 * @author timothyosullivan
 */

public interface ReviewFragmentCallbacks {
    public AbstractAssessmentModel getWizardModel();
    public void onEditScreenAfterReview(String pageKey);
}
