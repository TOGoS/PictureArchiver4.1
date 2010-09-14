package togos.picturearchiver4_1.comframework;

import togos.mf.api.ResponseCodes;
import togos.mf.api.ResponseSession;
import togos.mf.base.BaseResponse;

public class ResponseSessions
{
	public static ResponseSession OK_THANKS =
		togos.mf.base.ResponseSessions.createSingleResultSession(
			new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Thanks", "text/plain") );
}
