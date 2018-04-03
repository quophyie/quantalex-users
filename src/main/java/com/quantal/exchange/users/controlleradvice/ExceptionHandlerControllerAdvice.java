package com.quantal.exchange.users.controlleradvice;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.javashared.annotations.logger.InjectLogger;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.HttpException;

import static com.quantal.javashared.facades.AbstractBaseFacade.toRESTResponse;

/**
 * Created by dman on 08/07/2017.
 */
@RestController
@ControllerAdvice
//@Slf4j
public class ExceptionHandlerControllerAdvice {

    @InjectLogger
    private QuantalLogger logger;

    private MessageService messageService;

    @Autowired
    public ExceptionHandlerControllerAdvice(MessageService messageService) {
        this.messageService = messageService;

    }

   // @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    //@ResponseBody
    public ResponseEntity handleThrowable(final Throwable ex) {
        logger.error("Unexpected error", ex);
        ResponseEntity responseEntity = toRESTResponse(null,
                messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR),
                HttpStatus.INTERNAL_SERVER_ERROR);
        Exception businessEx = CommonUtils.extractBusinessException(ex);
        if (ex.getCause() instanceof NotFoundException) {
            responseEntity = toRESTResponse(null, ex.getMessage(), HttpStatus.NOT_FOUND);
            return responseEntity;
        } else if (businessEx instanceof AlreadyExistsException) {
            responseEntity = toRESTResponse(null, businessEx.getMessage(), HttpStatus.CONFLICT);
        } else if (businessEx instanceof NullPointerException) {
            responseEntity = toRESTResponse(null,
                    messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED),
                    HttpStatus.BAD_REQUEST);
        } else if (businessEx instanceof PasswordValidationException) {
            responseEntity = toRESTResponse(null, businessEx.getMessage(), HttpStatus.BAD_REQUEST);
        } else if (businessEx instanceof HttpException) {
            HttpStatus status = HttpStatus.valueOf(((HttpException) businessEx).code());
            responseEntity = toRESTResponse(null, businessEx.getMessage(), status);
        }

        logger.error(ex.getMessage(),ex);
        return responseEntity;
    }


}
