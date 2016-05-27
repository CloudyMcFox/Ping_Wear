package rageofachilles.Ping;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;

/**
 * Created by Ryan on 5/15/2016.
 */
public class ContactDialogPreference extends DialogPreference
{
    private Uri QUERY_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    private String CONTACT_ID = ContactsContract.Contacts._ID;
    private String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
    private String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
    private String PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
    private int MAX_STRING = 20;
    private EditText m_etNumberView;
    private SearchView m_etSearchView;
    private String selectedPhoneNumber;
    private ArrayList<Contact> m_contactList = null;
    private ArrayList<Contact> m_contactListSearchable = null;
    ListView m_lv = null;

    public ContactDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ContactDialogPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(!positiveResult) {
            return;
        }
        String number = m_etNumberView.getText().toString();
        if (null == number || number.equals("")) {
            return;
        }

        if (null != selectedPhoneNumber && !selectedPhoneNumber.equals("")) {
            ((SettingsActivity) this.getContext()).Update("phoneNumber", selectedPhoneNumber);
        } else {// null is expected when number is entered manually
            ((SettingsActivity) this.getContext()).Update("phoneNumber", number);
        }
        ((SettingsActivity)this.getContext()).Update("phoneNumberText", number);
        super.onDialogClosed(positiveResult);

    }

    private void init(Context context) {
        setPersistent(false);
        setDialogLayoutResource(R.layout.contacts_layout);

    }

    public ArrayList<Contact> getContacts()
    {
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = new String[]{ DISPLAY_NAME, HAS_PHONE_NUMBER, Phone.TYPE, PHONE_NUMBER};
        Cursor cursor = cr.query(QUERY_URI, projection, ContactsContract.Contacts.HAS_PHONE_NUMBER, null,  ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
        ArrayList<Contact> contacts = new ArrayList<>();

        String oldName = "";
        int count =0;
        while (cursor.moveToNext()) {

            String name = (cursor.getString(cursor.getColumnIndex(DISPLAY_NAME))).trim();
            final int typeIndex = cursor.getColumnIndex(Phone.TYPE);
            final int numberIndex = cursor.getColumnIndex(PHONE_NUMBER);
            boolean isMultiple = false;
            int type = cursor.getInt(typeIndex);
            String phoneNumber = cursor.getString(numberIndex);

            if(name.equals(oldName)) {
                isMultiple = true;
            } else {
                oldName = name;
            }
            contacts.add(new Contact(name, phoneNumber, TypeToString(type), isMultiple));
            count++;
        }
        Log.v("myTag",""+count);
        cursor.close();
        return contacts;
    }

    protected String TypeToString(int type)
    {
        switch (type) {
            case Phone.TYPE_HOME:
                return "Home";
            case Phone.TYPE_MOBILE:
                return "Mobile";
            case Phone.TYPE_WORK:
                return "Work";
            case Phone.TYPE_FAX_HOME:
                return "Home Fax";
            case Phone.TYPE_FAX_WORK:
                return "Work Fax";
            case Phone.TYPE_MAIN:
                return "Main";
            case Phone.TYPE_OTHER:
                return "Other";
            case Phone.TYPE_CUSTOM:
                return "Custom";
            case Phone.TYPE_PAGER:
                return "Pager";
            default:
                return "";
        }
    }

    public void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    protected void onBindDialogView(View view)
    {
        m_lv = ((ListView) view.findViewById(R.id.lvContact));
        m_contactList = getContacts();
        // copy contacts list to an editable one
        m_contactListSearchable = (ArrayList<Contact>)m_contactList.clone();

        m_etSearchView = ((SearchView) view.findViewById(R.id.etSearch));
        m_etNumberView = ((EditText) view.findViewById(R.id.etNumber)); //Save view to access it ondialogclose
        // Set the ArrayAdapter as the ListView's adapter.
        m_lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, m_contactListSearchable) );

        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(!(v instanceof EditText)) {
                    hideSoftKeyboard(v);
                }
                return false;
            }
        });

        // Set the etNumberView to the current number
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String number = prefs.getString("phoneNumberText","");
        m_etNumberView.setText(number);

        m_etNumberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                * Commenting this out due not allowing inserting characters in front of text from contact.
                * instead just remove text
                // Highlight the text:
                // From Stack Overflow:
                // Problem caused by IME. If showed cursor drag pointer then selection must be zero width.
                // You need cancel drag pointer. It can be done by change text.
                // We replace first symbol of text with same symbol. It cause cancel drag pointer and allow make selection without bug.
                etNumberView.selectAll();
                if (etNumberView.getText().length() > 0) {
                    etNumberView.getText().replace(0, 1, etNumberView.getText().subSequence(0, 1), 0, 1);
                    etNumberView.selectAll();
                }
                */
                m_etNumberView.setText("");
            }
        });
        m_etNumberView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    // hack hack... clear selectedPhoneNumber to indicate to OnDialogClose that a contact was not click, but number entered manually
                    selectedPhoneNumber = null;
                    onDialogClosed(true); // fake dialog close to update value.
                    getDialog().dismiss();
                    return true;
                }
                return false;
            }
        });

        m_etSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_etSearchView.onActionViewExpanded();
            }
        });

        m_etSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                m_contactListSearchable = (ArrayList<Contact>)m_contactList.clone();
                // Update contacts
                if(newText.equals("")){
                    m_lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, m_contactList));
                    return true;
                }

                for (int i =0; i < m_contactListSearchable.size(); i++) {
                    Contact cnt = m_contactListSearchable.get(i);
                    if (!cnt.getName().toLowerCase().contains(newText.toLowerCase())){ //if name does not match
                        String searchString = newText.toLowerCase().replaceAll("[\\D]",""); // for number check remove all non digits
                        if (searchString.equals("") || !cnt.getNumber().toLowerCase().replaceAll("[\\D]","").contains(searchString)){ // if number doesnt match

                            m_contactListSearchable.remove(i);
                            // since we removed one, lower i again
                            i--;
                        }
                    }
                }
                // Unfortunately this now broke the hidden names as a phone number search may have hit on a hidden name. Need to re loop through :(
                String oldName = "";
                for (int i =0; i < m_contactListSearchable.size(); i++) {
                    String newName = m_contactListSearchable.get(i).name;
                    if (0 == i) {
                        // set first one to true always
                        m_contactListSearchable.get(i).setIsMultiple(false);
                        oldName = newName;
                        continue;
                    }
                    m_contactListSearchable.get(i).setIsMultiple(oldName.equals(newName) ? true: false);
                    oldName = newName;
                }

                m_lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, m_contactListSearchable));
                return true;
            }
            public boolean onQueryTextSubmit(String query) {
                // Do something
                m_etSearchView.clearFocus();
                return true;
            }
        });


        super.onBindDialogView(view);
    }

    public class ContactAdapter extends ArrayAdapter<Contact>
    {
        private final Context context;
        private final ArrayList<Contact> data;
        private final int layoutResourceId;

        public ContactAdapter(Context context, int layoutResourceId, ArrayList<Contact> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
            this.layoutResourceId = layoutResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if(row == null) {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ViewHolder();
                holder.textView1 = (TextView)row.findViewById(R.id.lblName);
                holder.textView2 = (TextView)row.findViewById(R.id.lblType);
                holder.textView3 = (TextView)row.findViewById(R.id.lblNumber);
                row.setTag(holder);
            } else {
                holder = (ViewHolder)row.getTag();
            }
            final View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove keyboard if it exists
                    hideSoftKeyboard(v);
                    // first get the name
                    String number = ((TextView)v.findViewById(R.id.lblNumber)).getText().toString();
                    selectedPhoneNumber = number; // Save number to store in preferences on close
                    String name = ((TextView)v.findViewById(R.id.lblName)).getText().toString();
                    View parent = (View)v.getParent().getParent();// Contacts is first parent, Dialog is second.
                    EditText et = (EditText)parent.findViewById(R.id.etNumber);
                    if (null != et){
                        et.setText(name+ "- " + number);
                    }
                }
            };
            row.setOnClickListener(onClickListener);

            Contact person = data.get(position);

            String name = person.getName();
            if (name.length() > MAX_STRING) {
                name = name.substring(0, MAX_STRING);
                name = name.concat("...");
            }
            holder.textView1.setText(name);
            if ( person.getIsMultiple()){
                holder.textView1.setVisibility(View.INVISIBLE);
            } else {
                holder.textView1.setVisibility(View.VISIBLE);
            }
            holder.textView2.setText(person.getType());
            holder.textView3.setText(person.getNumber());
            return row;
        }

        class ViewHolder
        {
            TextView textView1;
            TextView textView2;
            TextView textView3;
        }
    }


    public class Contact
    {
        String name;
        String number;
        String type;
        boolean fIsMultiple = false; // Used to hide name label if its a multiple.

        public Contact (String input_name, String input_number, String input_type, boolean isMultiple) {
            name = input_name;
            number = input_number;
            type = input_type;
            fIsMultiple = isMultiple;
        }

        public String getName(){
            return name;
        }

        public String getNumber(){
            return number;
        }

        public String getType(){
            return type;
        }

        public boolean getIsMultiple(){
            return fIsMultiple;
        }

        public void setIsMultiple(boolean input){
            fIsMultiple = input;
        }
    }
}

