package works.deasil.magparse;
/*
 * This file is part of Deasil Works suite of Android components.
 *
 * Copyright (c) 2017 Deasil Works Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Process raw magData for validation and data extraction.
 * 
 */
public class MagParse {

	private String magData;
	private String cardNum;
	private String expiration;
	private String firstName;
	private String lastName;
	private boolean isValid;
	private String message;

	public MagParse(String magData) {
		this.magData = magData;
		this.cardNum = null;
		this.expiration = null;
		this.firstName = null;
		this.lastName = null;
		this.isValid = true;
		this.message = null;

		chop();
	}
	
	public void clear() {
		this.magData = null;
		this.cardNum = null;
		this.expiration = null;
		this.firstName = null;
		this.lastName = null;
		this.isValid = true;
		this.message = null;
	}

	private void chop() {

		Pattern r = Pattern.compile(
				"(%?([A-Z])([0-9]{1,19})\\^([^\\^]{2,26})\\^([0-9]{4}|\\^)([0-9]{3}|\\^)?([^\\?]+)?\\??)[\t\n\r ]{0,2}.*"
		);
		Matcher m = r.matcher(this.magData);

		String ccFormatCode;
		String ccNumber;
		String ccName;
		String ccExpiry;
        String invalidCard = "Invalid Card Type, please use a valid credit or debit card.";

		if (m.find()) {
			ccFormatCode = m.group(2);
			ccNumber = m.group(3);
			ccName = m.group(4);
			ccExpiry = m.group(5);
		} else {
            this.isValid = false;
			setMessage(invalidCard);
            return;
		}

		// Checking if this is a credit or debit card
		if (ccFormatCode.compareTo("B") != 0) {
            this.isValid = false;
			setMessage(invalidCard);
			return;
		}

		processCreditCardNum(ccNumber);
		processName(ccName);
		processExpiration(ccExpiry);
	}

	private boolean processCreditCardNum(String data) {

		if (luhnCheck(data)) {
			this.cardNum = data;
			return true;
		} else {
			this.isValid = false;
			setMessage("Invalid Credit Card Number");
			return false;
		}
	}

	private void processName(String data) {
		
		// some cards have names formatted as "LOY DARLA E" instead of "LOY/DARLA E"
		if (!data.contains("/")) {
			// wide magstripe using space between name fix.
			data = data.replaceFirst(" ", "/");
		} else if (data.substring(data.length() - 1).equals("/")) {
			// US Bank Debit card fix. Name part has unconventional formatting
			// Most are: ^LASTNAME/FIRSTNAME M^
			// US Bank Debit Cards are: ^FIRSTNAME M LASTNAME        /^
			data = data.replace("/", "").trim();
			int index = data.lastIndexOf(" ");
			// StringIndexOutOfBoundsException fix
			if (data.length() > 0 && index >= 0) {
				String last = data.substring(index);
				String first = data.substring(0, index);
				data = last.trim() + "/" + first.trim();
			}
		}

		String[] bits = data.split("/");

		if (bits.length > 1) {
			this.firstName = bits[1].trim();
			this.lastName = bits[0].trim();
		} else {
			this.lastName = data;
		}
	}

	private boolean processExpiration(String data) {

		if (data.length() < 4) {
			this.isValid = false;
			setMessage("Expiration date parsing error");
			return false;
		}

		this.expiration = data.substring(2, 4) + data.substring(0, 2);

		return true;
	}

	private boolean luhnCheck(String ccNumber) {
		int sum = 0;
		boolean alternate = false;
		for (int i = ccNumber.length() - 1; i >= 0; i--) {
			int n = Integer.parseInt(ccNumber.substring(i, i + 1));
			if (alternate) {
				n *= 2;
				if (n > 9) {
					n = (n % 10) + 1;
				}
			}
			sum += n;
			alternate = !alternate;
		}
		return (sum % 10 == 0);
	}

	public String getMagData() {
		return magData;
	}

	public String getCardNum() {
		return cardNum;
	}

	public String getExpiration() {
		return expiration;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public boolean isValid() {
		return isValid;
	}

	private void setMessage(String msg) {
		this.message = msg;
	}

	public String getMessage() {
		return message;
	}
}
