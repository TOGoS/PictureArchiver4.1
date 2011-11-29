package togos.picturearchiver4_1.comframework;

import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

public class Responses
{
	public static Response OK_THANKS = new BaseResponse(ResponseCodes.NORMAL, "Thanks", "text/plain");
}
