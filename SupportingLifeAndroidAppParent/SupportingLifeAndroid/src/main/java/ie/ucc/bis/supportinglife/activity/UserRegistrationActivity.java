package ie.ucc.bis.supportinglife.activity;

import ie.ucc.bis.supportinglife.R;
import ie.ucc.bis.supportinglife.communication.UserAuthenticationComms;
import ie.ucc.bis.supportinglife.ui.utilities.LoggerUtils;
import ie.ucc.bis.supportinglife.validation.Field;
import ie.ucc.bis.supportinglife.validation.Form;
import ie.ucc.bis.supportinglife.validation.NotEmptyValidation;
import ie.ucc.bis.supportinglife.validation.ValidationListener;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Activity responsible for registering HSA user on initial launch
 * of the application.
 *
 * @author timothyosullivan
 */

public class UserRegistrationActivity extends SupportingLifeBaseActivity {

	private final String LOG_TAG = "ie.ucc.bis.supportinglife.activity.UserRegistrationActivity";
	
	private EditText userLogin;
	private EditText userPassword;
	private Button clearButton;
	private Button submitButton;
	private UserAuthenticationAsyncTask userAuthenticationAsyncTask;
	
	// Form used for validation
    private Form form;
	
	/**
	 * OnCreate method is called when the activity is first created.
	 * 
	 * This is where all of the normal static set up should occur
	 * e.g. create views, bind data to lists, etc.
	 * 
	 * The method also provides a Bundle parameter containing the activity's
	 * previously frozen state (if there was one).
	 * 
	 * This method is always followed by onStart().
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_registration);
		
		initGraphicalElements();
		initValidationForm();
		initCallbacks();
	}
	
	private void initGraphicalElements() {
		setUserLogin((EditText) findViewById(R.id.user_registration_login_id));
		setUserPassword((EditText) findViewById(R.id.user_registration_login_password));
		setClearButton((Button) findViewById(R.id.user_registration_clear_button));
		setSubmitButton((Button) findViewById(R.id.user_registration_submit_button));
	}

	private void initValidationForm() {
        setForm(new Form(this));

        getForm().addField(Field.using(getUserLogin(), getResources().getString(R.string.user_registration_login_id)).validate(NotEmptyValidation.build(this)));
        getForm().addField(Field.using(getUserPassword(), getResources().getString(R.string.user_registration_login_password)).validate(NotEmptyValidation.build(this)));
    }
	
    private void initCallbacks() {
        getSubmitButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               submitLoginDetails();
            }
        });
        
        getClearButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	clearUserInput();
            }
        });
    }
    
	private void submitLoginDetails() {
		// register validation listeners on editable UI components such
		// any subsequent focus change by a user will trigger a validation
		// check
		registerValidationListeners();
		
		// perform validation check
		getForm().performValidation();
		
		if (getForm().isValid()) {
            Crouton.makeText(this, "valid form input", Style.CONFIRM).show();
            
            setUserAuthenticationAsyncTask(new UserAuthenticationAsyncTask());
            getUserAuthenticationAsyncTask().execute(new UserAuthenticationComms(getUserLogin().getText().toString(), getUserPassword().getText().toString()));
            
            // TODO: TEMP - MOVE TO A MORE LOGICAL PLACE
			// record hsa user type
//    		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//    		SharedPreferences.Editor preferenceEditor = settings.edit();
//			preferenceEditor.putString(USER_TYPE_KEY, HSA_USER);
//			preferenceEditor.commit();
            
			// TODO authenticate the user
			
//			startActivity(new Intent(getApplicationContext(), HomeActivity.class));	
			// configure the activity animation transition effect
//			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);	
            
            
        }		
	}
	
	private void clearUserInput() {	
		// clear any validation messages
		getForm().getValidationFailedRenderer().clearAll();
		
		// clear textual content
        getUserLogin().setText("");
        getUserPassword().setText("");
        
		// remove UI component validation listeners
		removeValidationListeners();
	}
	
	/**
	 * Responsible for registering validation listeners on editable UI components.
	 * 
	 * Generally registration of a validation listener on a UI component should be
	 * done subsequent to the initial validation check so that the user is not
	 * plagued with validation feedback when initially filling in the form data.
	 * 
	 * In the case of 'User Registration', validation listeners will only be placed
	 * on the UI components after the user has initially selected the 'submit' button.
	 *
	 */
	private void registerValidationListeners() {
        getUserLogin().setOnFocusChangeListener(new ValidationListener(getForm()));
        getUserPassword().setOnFocusChangeListener(new ValidationListener(getForm()));	
	}
	
	/**
	 * Responsible for removing validation listeners from editable UI components.
	 *
	 */
	private void removeValidationListeners() {
        getUserLogin().setOnFocusChangeListener(null);
        getUserPassword().setOnFocusChangeListener(null);	
	}
	
	/**
	 * onResume method is called when the activity will start interacting with the user.
	 * 
	 * At this point your activity is at the top of the activity stack, with user input going to it.
	 * 
	 * This method is always followed by onPause().
	 *
	 */
	@Override
	protected void onResume () {
		super.onResume ();
	}
	
	/**
	 * Determine if this activity should display an ActionBar when it is
	 * shown.
	 * 
	 * @return boolean
	 */
	@Override
	protected boolean shouldDisplayActionBar() {
		return false;
	}
	
	
	private class UserAuthenticationAsyncTask extends AsyncTask<UserAuthenticationComms, Boolean, Boolean> {
		
		private static final String REST_REQUEST = AWS_BASE_URL + "user/register";
				
		@Override
		protected Boolean doInBackground(UserAuthenticationComms... params) {
			RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());		
			for (UserAuthenticationComms userAuthenticationComm : params) {
			
				try {
					// The default timeout was resulting in the call to the 'restTemplate.postForObject(..)' method
					// call sometimes returning a null object and sometimes returning a correctly populated object.
					// Doubling the read timeout led to more reliability in obtaining a correctly populated object.
					// default timeout is 60 * 1000
					((HttpComponentsClientHttpRequestFactory)restTemplate.getRequestFactory()).setConnectTimeout(120 * 1000);
					((HttpComponentsClientHttpRequestFactory)restTemplate.getRequestFactory()).setReadTimeout(120 * 1000);
					restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
				
					Boolean authenticationResponse = restTemplate.postForObject(REST_REQUEST, userAuthenticationComm, Boolean.class);
					
					publishProgress(authenticationResponse);
				} catch (ResourceAccessException ex) {
					LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: doInBackground -- ResourceAccessException");
					LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: doInBackground -- " + ex.getMessage());
				} catch (RestClientException ex) {
					LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: doInBackground -- RestClientException");
					LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: doInBackground -- " + ex.getMessage());
				}
			}
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: onPostExecute -- USER AUTHENTICATION OPERATION SUCCESSFUL");
			}
			else {
				LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: onPostExecute -- USER AUTHENTICATION OPERATION UNSUCCESSFUL!");
			}
		}
		
		@Override
		protected void onProgressUpdate(Boolean... authenticationResponse) {
			if (authenticationResponse[0].booleanValue()) {
				LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: onProgressUpdate -- USER AUTHENTICATED SUCCESSFULLY");
			}
			else {
				LoggerUtils.i(LOG_TAG, "UserAuthenticationAsyncTask: onProgressUpdate -- USER AUTHENTICATED UNSUCCESSFULLY!");
			}
		}

		@Override
		protected void onPreExecute() {}		
	} // end of inner class 'UserAuthenticationAsyncTask'	
	
	public EditText getUserLogin() {
		return userLogin;
	}

	public void setUserLogin(EditText userLogin) {
		this.userLogin = userLogin;
	}

	public EditText getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(EditText userPassword) {
		this.userPassword = userPassword;
	}

	public Button getClearButton() {
		return clearButton;
	}

	public void setClearButton(Button clearButton) {
		this.clearButton = clearButton;
	}

	public Button getSubmitButton() {
		return submitButton;
	}

	public void setSubmitButton(Button submitButton) {
		this.submitButton = submitButton;
	}

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public UserAuthenticationAsyncTask getUserAuthenticationAsyncTask() {
		return userAuthenticationAsyncTask;
	}

	public void setUserAuthenticationAsyncTask(UserAuthenticationAsyncTask userAuthenticationAsyncTask) {
		this.userAuthenticationAsyncTask = userAuthenticationAsyncTask;
	}
} // end class
